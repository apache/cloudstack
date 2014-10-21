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
    var elems = {
        aclDialog: function(args) {
            var isDialog = args.isDialog;
            var actionArgs = args.actionArgs;
            var context = args.context;
            var $acl = $('<div>').addClass('acl').multiEdit(
                $.extend(true, {}, actionArgs.multiEdit, {
                    context: context
                })
            );

            // Show ACL dialog
            if (isDialog) {
                $acl.dialog({
                    title: _l('label.configure.network.ACLs'),
                    dialogClass: 'configure-acl',
                    width: 900,
                    height: 600,
                    buttons: {
                        'Done': function() {
                            $(':ui-dialog').remove();
                            $('.overlay').remove();
                        }
                    }
                });
            }

            return $acl;
        },
        vpcConfigureTooltip: function(args) {
            var context = args.context;
            var $browser = args.$browser;
            var $chart = args.$chart;
            var ipAddresses = args.ipAddresses;
            var acl = args.acl;
            var gateways = args.gateways;
            var siteToSiteVPN = args.siteToSiteVPN;
            var links = {
                'ip-addresses': _l('label.menu.ipaddresses'),
                'gateways': _l('label.private.Gateway'),
                'site-to-site-vpn': _l('label.site.to.site.VPN'),
                'network-acls': _l('label.network.ACLs')
            };
            var $links = $('<ul>').addClass('links');
            var $tooltip = $('<div>').addClass('vpc-configure-tooltip').append(
                $('<div>').addClass('arrow')
            );
            var tierDetailView = args.tierDetailView;

            // Make links
            $.map(links, function(label, id) {
                var $link = $('<li>').addClass('link').addClass(id);
                var $label = $('<span>').html(label);

                $link.append($label);
                $link.appendTo($links);

                // Link event
                $link.click(function() {
                    switch (id) {
                        case 'network-acls':
                            $browser.cloudBrowser('addPanel', {
                                title: _l('label.network.ACLs'),
                                maximizeIfSelected: true,
                                complete: function($panel) {
                                    $panel.listView(
                                        $.extend(true, {}, acl.listView, {
                                            listView: {
                                                actions: {
                                                    add: {
                                                        label: 'label.add.network.ACL',
                                                        action: {
                                                            custom: function() {
                                                                elems.aclDialog({
                                                                    isDialog: true,
                                                                    actionArgs: acl,
                                                                    context: context
                                                                });
                                                            }
                                                        }
                                                    }
                                                },
                                                detailView: function() {
                                                    var detailView = $.extend(true, {}, tierDetailView);

                                                    detailView.tabs = {
                                                        acl: tierDetailView.tabs.acl
                                                    };

                                                    return detailView;
                                                }
                                            }
                                        }), {
                                            context: acl.context
                                        }
                                    );
                                }
                            });
                            break;

                        case 'ip-addresses':
                            $browser.cloudBrowser('addPanel', {
                                title: _l('label.menu.ipaddresses'),
                                maximizeIfSelected: true,
                                complete: function($panel) {
                                    //ipAddresses.listView is a function
                                    $panel.listView(ipAddresses.listView(), {
                                        context: ipAddresses.context
                                    });
                                }
                            });
                            break;
                        case 'gateways':
                            //siteToSiteVPN is an object
                            var addAction = gateways.add;
                            var isGatewayPresent = addAction.preCheck({
                                context: gateways.context
                            });
                            var showGatewayListView = function() {
                                $browser.cloudBrowser('addPanel', {
                                    title: _l('label.private.Gateway'),
                                    maximizeIfSelected: true,
                                    complete: function($panel) {
                                        $panel.listView(gateways.listView(), {
                                            context: gateways.context
                                        });
                                    }
                                });
                            };

                            if (isGatewayPresent) {
                                showGatewayListView();
                            } else {
                                cloudStack.dialog.createForm({
                                    form: addAction.createForm,
                                    context: args.gateways.context,
                                    after: function(args) {
                                        var data = args.data;
                                        var error = function(message) {
                                            cloudStack.dialog.notice({
                                                message: message
                                            });
                                        };

                                        addAction.action({
                                            data: data,
                                            $form: args.$form,
                                            context: gateways.context,
                                            response: {
                                                success: function(args) {
                                                    var _custom = args._custom;
                                                    var notification = {
                                                        poll: addAction.notification.poll,
                                                        _custom: _custom,
                                                        desc: addAction.messages.notification()
                                                    };
                                                    var success = function(args) {
                                                        if (!$chart.is(':visible')) return;

                                                        cloudStack.dialog.confirm({
                                                            message: 'Gateway for VPC has been created successfully. Would you like to see its details?',
                                                            action: showGatewayListView
                                                        });
                                                    };

                                                    cloudStack.dialog.notice({
                                                        message: 'Your gateway is being created; please see notifications window.'
                                                    });

                                                    cloudStack.ui.notifications.add(
                                                        notification,
                                                        success, {},
                                                        error, {}
                                                    );
                                                },
                                                error: error
                                            }
                                        });
                                    }
                                });
                            }
                            break;
                        case 'site-to-site-vpn':
                            //siteToSiteVPN is an object
                            var addAction = siteToSiteVPN.add;
                            var isVPNPresent = addAction.preCheck({
                                context: siteToSiteVPN.context
                            });
                            var showVPNListView = function() {
                                $browser.cloudBrowser('addPanel', {
                                    title: _l('label.site.to.site.VPN'),
                                    maximizeIfSelected: true,
                                    complete: function($panel) {
                                        $panel.listView(siteToSiteVPN, {
                                            context: siteToSiteVPN.context
                                        });
                                    }
                                });
                            };

                            if (isVPNPresent) {
                                showVPNListView();
                            } else {
                                cloudStack.dialog.confirm({
                                    message: 'Please confirm that you want to add a Site-to-Site VPN gateway.',
                                    action: function() {
                                        var error = function(message) {
                                            cloudStack.dialog.notice({
                                                message: message
                                            });
                                        };

                                        addAction.action({
                                            context: siteToSiteVPN.context,
                                            response: {
                                                success: function(args) {
                                                    var _custom = args._custom;
                                                    var notification = {
                                                        poll: addAction.notification.poll,
                                                        _custom: _custom,
                                                        desc: addAction.messages.notification()
                                                    };
                                                    var success = function(args) {
                                                        if (!$chart.is(':visible')) return;
                                                        cloudStack.dialog.confirm({
                                                            message: 'Gateway for VPC has been created successfully. Would you like to see its details?',
                                                            action: showVPNListView
                                                        });
                                                    };

                                                    cloudStack.dialog.notice({
                                                        message: 'Your VPN is being created; please see notifications window.'
                                                    });

                                                    cloudStack.ui.notifications.add(
                                                        notification,
                                                        success, {},
                                                        error, {}
                                                    );
                                                },
                                                error: error
                                            }
                                        });
                                    }
                                });
                            }
                            break;
                    }
                });
            });

            $tooltip.append($links);

            // Tooltip hover event
            $tooltip.hover(
                function() {
                    $tooltip.addClass('active');
                },
                function() {
                    $tooltip.removeClass('active');

                    setTimeout(function() {
                        if (!$tooltip.hasClass('active')) {
                            $tooltip.remove();
                        }
                    }, 500);
                }
            );

            return $tooltip;
        },
        vpcConfigureArea: function(args) {
            var context = args.context;
            var $browser = args.$browser;
            var $chart = args.$chart;
            var ipAddresses = args.ipAddresses;
            var acl = args.acl;
            var gateways = args.gateways;
            var siteToSiteVPN = args.siteToSiteVPN;
            var $config = $('<div>').addClass('config-area');
            var $configIcon = $('<span>').addClass('icon').html('&nbsp');
            var tierDetailView = args.tierDetailView;

            $config.append($configIcon);

            // Tooltip event
            $configIcon.mouseover(function() {
                var $tooltip = elems.vpcConfigureTooltip({
                    context: context,
                    $browser: $browser,
                    $chart: $chart,
                    ipAddresses: ipAddresses,
                    gateways: gateways,
                    acl: acl,
                    siteToSiteVPN: siteToSiteVPN,
                    tierDetailView: tierDetailView
                });

                // Make sure tooltip is center aligned with icon
                $tooltip.css({
                    left: $configIcon.position().left
                });
                $tooltip.appendTo($config).hide();
                $tooltip.stop().fadeIn('fast');
            });

            return $config;
        },
        router: function(args) {
            var $browser = args.$browser;
            var detailView = args.detailView;
            var $router = $('<li>').addClass('tier virtual-router');
            var $title = $('<span>').addClass('title').html(_l('label.virtual.router'));

            $router.append($title);

            // Append horizontal chart line
            $router.append($('<div>').addClass('connect-line'));

            $router.click(function() {
                if ($router.hasClass('disabled')) return false;

                $browser.cloudBrowser('addPanel', {
                    title: _l('label.VPC.router.details'),
                    complete: function($panel) {
                        $panel.detailView(detailView);
                    }
                });
            });

            return $router;
        },
        tier: function(args) {
            var ipAddresses = args.ipAddresses;
            var acl = args.acl;
            var name = args.name;
            var cidr = args.cidr;
            var context = args.context;
            var vmListView = args.vmListView;
            var actionPreFilter = args.actionPreFilter;
            var actions = $.map(
                args.actions ? args.actions : {}, function(value, key) {
                    return {
                        id: key,
                        action: value
                    };
                }
            );
            var detailView = args.detailView;
            var $browser = args.$browser;
            var isPlaceholder = args.isPlaceholder;
            var virtualMachines = args.virtualMachines;
            var $tier = $('<li>').addClass('tier');
            var $title = $('<span>').addClass('title');
            var $cidr = $('<span>').addClass('cidr');
            var $vmCount = $('<span>').addClass('vm-count');
            var $actions = $('<div>').addClass('actions');

            // Ignore special actions
            // -- Add tier action is handled separately
            actions = $.grep(actions, function(action) {
                return action.id != 'add';
            });

            // Add loading indicator
            $vmCount.append(
                $('<div>').addClass('loading-overlay')
                .attr('title', 'VMs are launching in this tier.')
            );

            // VM count shows instance list
            $vmCount.click(function() {
                var $dialog = $('<div>');
                var $listView = $('<div>').listView($.extend(true, {}, vmListView, {
                    context: context
                }));

                $dialog.append($listView);
                $dialog.dialog({
                    title: _l('label.VMs.in.tier') + ': ' + name,
                    dialogClass: 'multi-edit-add-list panel configure-acl',
                    width: 825,
                    height: 600,
                    buttons: {
                        'Done': function() {
                            $(':ui-dialog').remove();
                            $('.overlay').remove();
                        }
                    }
                }).closest('.ui-dialog').overlay();
            });

            // Title shows tier details
            $title.click(function() {
                if ($browser && $browser.size()) { // Fix null exception, if add tier returns error
                    $browser.cloudBrowser('addPanel', {
                        title: name,
                        maximizeIfSelected: true,
                        complete: function($panel) {
                            $panel.detailView($.extend(true, {}, detailView, {
                                $browser: $browser,
                                context: context
                            }));
                        }
                    });
                }
            });

            if (isPlaceholder) {
                $tier.addClass('placeholder');
                $title.html(_l('label.add.new.tier'));
            } else {
                $title.html(
                    cloudStack.concat(name, 8)
                );
                $title.attr('title', name);
                $cidr.html(cidr);
                $vmCount.append(
                    $('<span>').addClass('total').html(virtualMachines != null ? virtualMachines.length : 0),
                    _l('label.vms')
                );
                $tier.append($actions);

                // Build action buttons
                $(actions).map(function(index, action) {
                    var $action = $('<div>').addClass('action');
                    var shortLabel = action.action.shortLabel;
                    var label = action.action.label;

                    $action.addClass(action.id);

                    if (action.id != 'remove') {
                        $action.append($('<span>').addClass('label').html(_l(shortLabel)));
                    } else {
                        $action.append($('<span>').addClass('icon').html('&nbsp;'));
                    }

                    $actions.append($action);
                    $action.attr('title', label);
                    $action.data('vpc-tier-action-id', action.id);

                    // Action event
                    $action.click(function() {
                        if ($action.hasClass('disabled')) {
                            return false;
                        }

                        tierAction({
                            action: action,
                            actionPreFilter: actionPreFilter,
                            context: context,
                            $tier: $tier,
                            $actions: $actions
                        });

                        return true;
                    });
                });
            }

            $tier.prepend($title);

            if (!isPlaceholder) {
                $tier.append($title, $cidr, $vmCount);
            }

            // Append horizontal chart line
            $tier.append($('<div>').addClass('connect-line'));

            // Handle action filter
            filterActions({
                $actions: $actions,
                actionPreFilter: actionPreFilter,
                context: context
            });

            return $tier;
        },
        chart: function(args) {
            var $browser = args.$browser;
            var acl = args.acl;
            var ipAddresses = args.ipAddresses;
            var gateways = args.gateways;
            var siteToSiteVPN = args.siteToSiteVPN;
            var tiers = args.tiers;
            var vmListView = args.vmListView;
            var actions = args.actions;
            var actionPreFilter = args.actionPreFilter;
            var vpcName = args.vpcName;
            var context = args.context;
            var tierDetailView = args.tierDetailView;
            var $tiers = $('<ul>').addClass('tiers');
            var $router;

            $router = elems.router({
                $browser: $browser,
                detailView: $.extend(true, {}, args.routerDetailView(), {
                    context: context
                })
            });

            if (!isAdmin()) $router.addClass('disabled');

            var $chart = $('<div>').addClass('vpc-chart');
            var $title = $('<div>').addClass('vpc-title')
                .append(
                    $('<span>').html(vpcName)
            )
                .append(
                    elems.vpcConfigureArea({
                        context: context,
                        $browser: $browser,
                        $chart: $chart,
                        ipAddresses: $.extend(ipAddresses, {
                            context: context
                        }),
                        gateways: $.extend(gateways, {
                            context: context
                        }),
                        siteToSiteVPN: $.extend(siteToSiteVPN, {
                            context: context
                        }),
                        acl: $.extend(acl, {
                            context: context
                        }),
                        tierDetailView: tierDetailView
                    })
            );

            var showAddTierDialog = function() {
                if ($(this).find('.loading-overlay').size()) {
                    return false;
                }

                addTierDialog({
                    ipAddresses: ipAddresses,
                    $browser: $browser,
                    tierDetailView: tierDetailView,
                    $tiers: $tiers,
                    acl: acl,
                    context: context,
                    actions: actions,
                    vmListView: vmListView,
                    actionPreFilter: actionPreFilter
                });

                return true;
            };

            if (tiers != null && tiers.length > 0) {
                $(tiers).map(function(index, tier) {
                    var $tier = elems.tier({
                        ipAddresses: ipAddresses,
                        acl: acl,
                        $browser: $browser,
                        detailView: tierDetailView,
                        name: tier.name,
                        cidr: tier.cidr,
                        virtualMachines: tier.virtualMachines,
                        vmListView: vmListView,
                        actions: actions,
                        actionPreFilter: actionPreFilter,
                        context: $.extend(true, {}, context, {
                            networks: [tier]
                        })
                    });

                    $tier.appendTo($tiers);
                });

            }

            elems.tier({
                isPlaceholder: true
            }).appendTo($tiers)
                .click(showAddTierDialog);
            $tiers.prepend($router);
            $chart.append($title, $tiers);

            if (!tiers || !tiers.length) {
                showAddTierDialog();
            }

            return $chart;
        }
    };

    var filterActions = function(args) {
        var $actions = args.$actions;
        var actionPreFilter = args.actionPreFilter;
        var context = args.context;
        var disabledActions, allowedActions;

        disabledActions = actionPreFilter ? actionPreFilter({
            context: context
        }) : [];

        // Visual appearance for disabled actions
        $actions.find('.action').map(function(index, action) {
            var $action = $(action);
            var actionID = $action.data('vpc-tier-action-id');

            if ($.inArray(actionID, disabledActions) > -1) {
                $action.addClass('disabled');
            } else {
                $action.removeClass('disabled');
            }
        });
    };

    // Handles tier action, including UI effects
    var tierAction = function(args) {
        var $tier = args.$tier;
        var $loading = $('<div>').addClass('loading-overlay');
        var $actions = args.$actions;
        var actionArgs = args.action.action;
        var action = actionArgs.action;
        var actionID = args.action.id;
        var notification = actionArgs.notification;
        var label = actionArgs.label;
        var context = args.context;
        var actionPreFilter = args.actionPreFilter;

        var success = function(args) {
            var remove = args ? args.remove : false;
            var _custom = args ? args._custom : {};

            var updateVMLoadingState = function() {
                var pendingVMs = $tier.data('vpc-tier-pending-vms');

                pendingVMs = pendingVMs ? pendingVMs - 1 : 0;

                if (!pendingVMs) {
                    $tier.data('vpc-tier-pending-vms', 0);
                    $tier.removeClass('loading');
                } else {
                    $tier.data('vpc-tier-pending-vms', pendingVMs);
                }
            };

            cloudStack.ui.notifications.add(
                // Notification
                {
                    desc: label,
                    poll: notification.poll,
                    _custom: _custom
                },

                // Success

                function(args) {
                    if (actionID == 'addVM') {
                        // Increment VM total
                        var $total = $tier.find('.vm-count .total');
                        var prevTotal = parseInt($total.html());
                        var newTotal = prevTotal + 1;
                        var newVM = args.data;
                        var newContext = $.extend(true, {}, context, {
                            vms: [newVM]
                        });

                        $total.html(newTotal);

                        filterActions({
                            $actions: $actions,
                            actionPreFilter: actionPreFilter,
                            context: newContext
                        });

                        updateVMLoadingState();
                    } else if (actionID == 'remove') { //remove tier
                        $loading.remove();
                        $tier.remove();
                    } else {
                        $loading.remove();
                    }

                },

                {},

                // Error

                function(args) {
                    if (actionID == 'addVM') {
                        updateVMLoadingState();
                    } else {
                        $loading.remove();
                    }
                }
            );
        };

        switch (actionID) {
            case 'addVM':
                action({
                    context: context,
                    complete: function(args) {
                        var pendingVMs = $tier.data('vpc-tier-pending-vms');

                        pendingVMs = pendingVMs ? pendingVMs + 1 : 1;
                        $tier.addClass('loading');
                        $tier.data('vpc-tier-pending-vms', pendingVMs);
                        success(args);
                    }
                });
                break;
            case 'remove':
                $loading.appendTo($tier);
                action({
                    context: context,
                    response: {
                        success: function(args) {
                            success($.extend(args, {
                                remove: true
                            }));
                        }
                    }
                });
                break;
            case 'acl':
                elems.aclDialog({
                    isDialog: true,
                    actionArgs: actionArgs,
                    context: context
                }).closest('.ui-dialog').overlay();
                break;
            default:
                $loading.appendTo($tier);
                action({
                    context: context,
                    complete: success,
                    response: {
                        success: success,
                        error: function(args) {
                            $loading.remove();
                        }
                    }
                });
        }
    };

    // Appends a new tier to chart
    var addNewTier = function(args) {
        var ipAddresses = args.ipAddresses;
        var acl = args.acl;
        var actions = args.actions;
        var vmListView = args.vmListView;
        var actionPreFilter = args.actionPreFilter;
        var context = args.context;
        var $browser = args.$browser;
        var tierDetailView = args.tierDetailView;
        var tier = $.extend(args.tier, {
            ipAddresses: ipAddresses,
            $browser: $browser,
            detailView: tierDetailView,
            context: context,
            vmListView: vmListView,
            actions: actions,
            actionPreFilter: actionPreFilter,
            acl: acl,
            virtualMachines: []
        });
        var $tiers = args.$tiers;

        $tiers.find('li.placeholder')
            .before(
                elems.tier(tier)
                .hide()
                .fadeIn('slow')
        );
    };

    // Renders the add tier form, in a dialog
    var addTierDialog = function(args) {
        var ipAddresses = args.ipAddresses;
        var actions = args.actions;
        var context = args.context;
        var vmListView = args.vmListView;
        var actionPreFilter = args.actionPreFilter;
        var $tiers = args.$tiers;
        var $browser = args.$browser;
        var tierDetailView = args.tierDetailView;
        var acl = args.acl;

        cloudStack.dialog.createForm({
            context: context,
            form: actions.add.createForm,
            after: function(args) {
                var $loading = $('<div>').addClass('loading-overlay').prependTo($tiers.find('li.placeholder'));
                actions.add.action({
                    context: context,
                    data: args.data,
                    response: {
                        success: function(args) {
                            var tier = args.data;

                            cloudStack.ui.notifications.add(
                                // Notification
                                {
                                    desc: actions.add.label
                                },

                                // Success

                                function(args) {
                                    $loading.remove();
                                    addNewTier({
                                        ipAddresses: ipAddresses,
                                        $browser: $browser,
                                        tierDetailView: tierDetailView,
                                        context: $.extend(true, {}, context, {
                                            networks: [tier]
                                        }),
                                        tier: tier,
                                        acl: acl,
                                        $tiers: $tiers,
                                        actions: actions,
                                        actionPreFilter: actionPreFilter,
                                        vmListView: vmListView
                                    });
                                },

                                {},

                                // Error

                                function(args) {
                                    $loading.remove();
                                }
                            );
                        },
                        error: function(errorMsg) {
                            cloudStack.dialog.notice({
                                message: _s(errorMsg)
                            });
                            $loading.remove();
                        }
                    }
                });
            }
        });
    };

    cloudStack.uiCustom.vpc = function(args) {
        var vmListView = args.vmListView;
        var tierArgs = args.tiers;
        var ipAddresses = args.ipAddresses;
        var gateways = args.gateways;
        var acl = args.acl;
        var siteToSiteVPN = args.siteToSiteVPN;
        var routerDetailView = args.routerDetailView;

        return function(args) {
            var context = args.context;
            var $browser = $('#browser .container');
            var $toolbar = $('<div>').addClass('toolbar');
            var vpc = args.context.vpc[0];

            $browser.cloudBrowser('addPanel', {
                maximizeIfSelected: true,
                title: _l('label.configure.vpc') + ': ' + vpc.name,
                complete: function($panel) {
                    var $loading = $('<div>').addClass('loading-overlay').appendTo($panel);

                    $panel.append($toolbar);

                    // Load data
                    tierArgs.dataProvider({
                        context: context,
                        response: {
                            success: function(args) {
                                // Setup detail view tabs
                                var tierDetailView = $.extend(true, {}, tierArgs.detailView, {
                                    tabs: {
                                        acl: {
                                            custom: function(args) {
                                                var $acl = elems.aclDialog({
                                                    isDialog: false,
                                                    actionArgs: acl,
                                                    context: args.context
                                                });

                                                return $acl;
                                            }
                                        },
                                        ipAddresses: {
                                            custom: function(args) {
                                                return $('<div>').listView(ipAddresses.listView(), {
                                                    context: args.context
                                                });
                                            }
                                        }
                                    }
                                });

                                var tiers = args.tiers;
                                var $chart = elems.chart({
                                    $browser: $browser,
                                    ipAddresses: ipAddresses,
                                    gateways: gateways,
                                    acl: acl,
                                    tierDetailView: tierDetailView,
                                    routerDetailView: routerDetailView,
                                    siteToSiteVPN: siteToSiteVPN,
                                    vmListView: vmListView,
                                    context: context,
                                    actions: tierArgs.actions,
                                    actionPreFilter: tierArgs.actionPreFilter,
                                    vpcName: vpc.name,
                                    tiers: tiers
                                }).appendTo($panel);

                                $loading.remove();
                                $chart.fadeIn(function() {});
                            }
                        }
                    });
                }
            });
        };
    };
}(jQuery, cloudStack));
