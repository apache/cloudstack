(function(cloudStack, $) {

    cloudStack.routingPolicyTermWizard = {
        customUI: {
            addRoutingPolicyFromTerm: function(args) {
                var multiEditData = [];
                var totalIndex = 0;

                return $('<div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                        'prefix': {
                            edit: true,
                            label: 'label.prefix'
                        },
                        'prefixType': {
                            edit: true,
                            label: 'label.prefix.type',
                            select: function(args) {
                                var items = [];
                                items.push({
                                    name: "exact",
                                    description: "exact"
                                }),
                                    items.push({
                                        name: "longer",
                                        description: "longer"
                                    }),
                                    items.push({
                                        name: "orlonger",
                                        description: "orlonger"
                                    }),
                                    args.response.success({
                                        data: items
                                    });
                            }
                        },
                        'add-rule': {
                            label: 'label.add',
                            addButton: true
                        }
                    },
                    add: {
                        label: 'label.add',
                        action: function(args) {
                            multiEditData.push($.extend(args.data, {
                                index: totalIndex
                            }));

                            totalIndex++;
                            args.response.success();

                        }
                    },
                    actions: {
                        destroy: {
                            label: 'label.remove.routing.policy.term',
                            action: function(args) {
                                multiEditData = $.grep(multiEditData, function(item) {
                                    return item.index != args.context.multiRule[0].index;
                                });
                                args.response.success();
                            }
                        }
                    },
                    dataProvider: function(args) {
                        args.response.success({
                            data: multiEditData
                        });
                    }
                });
            },

            addRoutingPolicyThenTerm: function(args) {

                var multiEditData = [];
                var totalIndex = 0;

                return $('<div>').multiEdit({
                    context: args.context,
                    noSelect: true,
                    fields: {
                        'termType': {
                            edit: true,
                            label: 'label.term.type',
                            select: function(args) {
                                var items = [];
                                items.push({
                                    name: "add community",
                                    description: "add community"
                                }),
                                    items.push({
                                        name: "set community",
                                        description: "set community"
                                    }),
                                    items.push({
                                        name: "remove community",
                                        description: "remove community"
                                    }),
                                    items.push({
                                        name: "local-preference",
                                        description: "local-preference"
                                    }),
                                    items.push({
                                        name: "med",
                                        description: "med"
                                    }),
                                    items.push({
                                        name: "action",
                                        description: "action"
                                    }),
                                    items.push({
                                        name: "as-path",
                                        description: "as-path"
                                    }),
                                    args.response.success({
                                        data: items
                                    });
                                args.$select.change(function () {
                                    var $form = $(this).closest('form');
                                    var selectedOperation = $(this).val();
                                    if (selectedOperation === "action") {
                                        $form.find('[rel=termValue]').hide();
                                        $form.find('[rel=termAction]').show();
                                    } else {
                                        $form.find('[rel=termValue]').show();
                                        $form.find('[rel=termAction]').hide();
                                    }
                                });
                            }
                        },
                        'termValue': {
                            edit: true,
                            label: 'label.value'
                        },
                        'termAction': {
                            edit: true,
                            label: 'label.action',
                            isHidden: true,
                            select: function(args) {
                                var items = [];
                                items.push({
                                    name: "default",
                                    description: "default"
                                }),
                                    items.push({
                                        name: "reject",
                                        description: "reject"
                                    }),
                                    items.push({
                                        name: "accept",
                                        description: "accept"
                                    }),
                                    items.push({
                                        name: "next",
                                        description: "next"
                                    }),
                                    args.response.success({
                                        data: items
                                    });
                            }
                        },
                        'add-rule': {
                            label: 'label.add',
                            addButton: true
                        }
                    },
                    add: {
                        label: 'label.add',
                        action: function(args) {
                            if(args.data.termType === 'add community' || args.data.termType === "set community" || args.data.termType === 'remove community') {
                                var pattern=/^[0-9]+:[0-9]+$/;
                                if(!pattern.test(args.data.termValue)) {
                                    args.response.error("Community need to have the following format number:number");
                                    return;
                                }
                            }
                            if(args.data.termValue !== undefined) {
                                args.data.termAction = null;
                            }
                            multiEditData.push($.extend(args.data, {
                                index: totalIndex
                            }));

                            totalIndex++;
                            args.response.success();
                        }
                    },
                    actions: {
                        destroy: {
                            label: 'label.remove.routing.policy.term',
                            action: function(args) {
                                multiEditData = $.grep(multiEditData, function(item) {
                                    return item.index != args.context.multiRule[0].index;
                                });
                                args.response.success();
                            }
                        }
                    },
                    dataProvider: function(args) {
                        args.response.success({
                            data: multiEditData
                        });
                    }
                });
            },
        },

        preFilters: {
            routingPolicyFromTerm: function(args) {
                var $addTungstenRoutingPolicyTermFrom = $('.routing-policy-term-wizard:visible').find('#add_tungsten_routing_policy_from_term');
                $addTungstenRoutingPolicyTermFrom.find('#term_from').css('display', 'inline');
                return true;
            },
            routingPolicyThenTerm: function(args) {
                return true;
            }
        },

        forms: {
            routingPolicyFromTerm: {
                fields: {
                    community: {
                        label: 'label.community',
                        isTokenInput: true,
                        dataProvider: function (args) {
                            var items = [];
                            items.push({
                                id: "no-export",
                                name: "no-export"
                            });
                            items.push({
                                id: "no-export-subconfed",
                                name: "no-export-subconfed"
                            });
                            items.push({
                                id: "accept-own",
                                name: "accept-own"
                            });
                            items.push({
                                id: "no-advertise",
                                name: "no-advertise"
                            });
                            items.push({
                                id: "no-reoriginate",
                                name: "no-reoriginate"
                            });
                            var tags = [];
                            if (items != null) {
                                tags = $.map(items, function (tag) {
                                    return {
                                        id: tag.name,
                                        name: tag.name
                                    };
                                });
                            }

                            args.response.success({
                                data: tags,
                                hintText: _l('hint.type.part.tungsten.communities'),
                                noResultsText: _l('hint.no.tungsten.community')
                            });
                        },
                        desc: 'message.tooltip.community'
                    },
                    matchAllCommunities: {
                        label: 'label.match.all',
                        isBoolean: true,
                        desc: 'message.tooltip.match.all'
                    },
                    protocol: {
                        label: 'label.protocol',
                        isTokenInput: true,
                        dataProvider: function (args) {
                            var items = [];
                            items.push({
                                id: "bgp",
                                name: "bgp"
                            });
                            items.push({
                                id: "xmpp",
                                name: "xmpp"
                            });
                            items.push({
                                id: "static",
                                name: "static"
                            });
                            items.push({
                                id: "service-chain",
                                name: "service-chain"
                            });
                            items.push({
                                id: "aggregate",
                                name: "aggregate"
                            });
                            items.push({
                                id: "interface",
                                name: "interface"
                            });
                            items.push({
                                id: "interface-static",
                                name: "interface-static"
                            });
                            items.push({
                                id: "service-interface",
                                name: "service-interface"
                            });
                            items.push({
                                id: "BGPaaS",
                                name: "BGPaaS"
                            });
                            var tags = [];
                            if (items != null) {
                                tags = $.map(items, function (tag) {
                                    return {
                                        id: tag.name,
                                        name: tag.name
                                    };
                                });
                            }

                            args.response.success({
                                data: tags,
                                hintText: _l('hint.type.part.tungsten.protocol'),
                                noResultsText: _l('hint.no.tungsten.protocol')
                            });
                        },
                        desc: 'message.tooltip.protocol'
                    }
                }
            },
            routingPolicyThenTerm: {
                termType: {
                    label: 'label.term.type'
                },
                termValue: {
                    label: 'label.term.value',
                    validation: {
                        required: true
                    }
                },
                termAction: {
                    label: 'label.term.action'
                }
            }
        }
    };
}(cloudStack, jQuery));
