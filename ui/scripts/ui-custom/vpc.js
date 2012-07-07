// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
(function($, cloudStack) {
  var elems = {
    vpcConfigureTooltip: function(args) {
      var $browser = args.$browser;
      var siteToSiteVPN = args.siteToSiteVPN;
      var links = {
        'ip-addresses': 'IP Addresses',
        'gateways': 'Gateways',
        'site-to-site-vpn': 'Site-to-site VPN'
      };
      var $links = $('<ul>').addClass('links');
      var $tooltip = $('<div>').addClass('vpc-configure-tooltip').append(
        $('<div>').addClass('arrow')
      );

      // Make links
      $.map(links, function(label, id) {
        var $link = $('<li>').addClass('link').addClass(id);
        var $label = $('<span>').html(label);

        $link.append($label);
        $link.appendTo($links);

        // Link event
        $link.click(function() {
          switch (id) {
          case 'site-to-site-vpn':
            $browser.cloudBrowser('addPanel', {
              title: 'Site-to-site VPNs',
              maximizeIfSelected: true,
              complete: function($panel) {
                $panel.listView(
                  $.isFunction(siteToSiteVPN.listView) ?
                    siteToSiteVPN.listView() : siteToSiteVPN.listView
                );
              }
            });
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
      var $browser = args.$browser;
      var siteToSiteVPN = args.siteToSiteVPN;
      var $config = $('<div>').addClass('config-area');
      var $configIcon = $('<span>').addClass('icon').html('&nbsp');

      $config.append($configIcon);

      // Tooltip event
      $configIcon.mouseover(function() {
        var $tooltip = elems.vpcConfigureTooltip({
          $browser: $browser,
          siteToSiteVPN: siteToSiteVPN
        });

        // Make sure tooltip is center aligned with icon
        $tooltip.css({ left: $configIcon.position().left });
        $tooltip.appendTo($config).hide();
        $tooltip.stop().fadeIn('fast');
      });

      return $config;
    },
    router: function() {
      var $router = $('<li>').addClass('tier virtual-router');
      var $title = $('<span>').addClass('title').html('Virtual Router');

      $router.append($title);

      // Append horizontal chart line
      $router.append($('<div>').addClass('connect-line'));

      return $router;
    },
    tier: function(args) {
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

      // VM count shows instance list
      $vmCount.click(function() {
        var $dialog = $('<div>');
        var $listView = $('<div>').listView($.extend(true, {}, vmListView, {
          context: context
        }));

        $dialog.append($listView);
        $dialog.dialog({
          title: 'VMs in this tier',
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

      if (isPlaceholder) {
        $tier.addClass('placeholder');
        $title.html('Create Tier');
      } else {
        $title.html(name);
        $cidr.html(cidr);
        $vmCount.append(
          $('<span>').addClass('total').html(virtualMachines != null? virtualMachines.length: 0),
          ' VMs'
        );
        $tier.append($actions);

        // Build action buttons
        $(actions).map(function(index, action) {
          var $action = $('<div>').addClass('action');
          var shortLabel = action.action.shortLabel;
          var label = action.action.label;

          $action.addClass(action.id);

          if (action.id != 'remove') {
            $action.append($('<span>').addClass('label').html(shortLabel));
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
      var siteToSiteVPN = args.siteToSiteVPN;
      var tiers = args.tiers;
      var vmListView = args.vmListView;
      var actions = args.actions;
      var actionPreFilter = args.actionPreFilter;
      var vpcName = args.vpcName;
      var context = args.context;
      var $tiers = $('<ul>').addClass('tiers');
      var $router = elems.router();
      var $chart = $('<div>').addClass('vpc-chart');
      var $title = $('<div>').addClass('vpc-title')
            .append(
              $('<span>').html(vpcName)
            )
            .append(
              elems.vpcConfigureArea({
                $browser: $browser,
                siteToSiteVPN: siteToSiteVPN
              })
            );

      var showAddTierDialog = function() {
        if ($(this).find('.loading-overlay').size()) {
          return false;
        }

        addTierDialog({
          $tiers: $tiers,
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
            name: tier.name,
            cidr: tier.cidr,
            virtualMachines: tier.virtualMachines,
            vmListView: vmListView,
            actions: actions,
            actionPreFilter: actionPreFilter,
            context: $.extend(true, {}, context, {
              tiers: [tier]
            })
          });

          $tier.appendTo($tiers);
        });

      }

      elems.tier({ isPlaceholder: true }).appendTo($tiers)
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
						$total.html(newTotal);
						
						$loading.remove();
						
						var newVM = args.data;		
            var newContext = $.extend(true, {}, context, {
						  vms: [newVM]
						});			
						filterActions({
							$actions: $actions,
							actionPreFilter: actionPreFilter,
							context: newContext
						});						
					}	
					
					else if (actionID == 'remove') { //remove tier		
					  $tier.remove();
					}				
					
        },

        {},

        // Error
        function(args) {
          $loading.remove();
        }
      );
    };

    switch(actionID) {
      case 'addVM':
        action({
          context: context,
          complete: function(args) {
            $loading.appendTo($tier);
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
      // Show ACL dialog
      $('<div>').multiEdit(
        $.extend(true, {}, actionArgs.multiEdit, {
          context: context
        })
      ).dialog({
        title: 'Configure ACL',
        dialogClass: 'configure-acl',
        width: 820,
        height: 600,
        buttons: {
          'Done': function() {
            $(':ui-dialog').remove();
            $('.overlay').remove();
          }
        }
      }).closest('.ui-dialog').overlay();
      break;
      default:
        $loading.appendTo($tier);
        action({
          context: context,
          complete: success,
          response: {
            success: success,
            error: function(args) { $loading.remove(); }
          }
        });
    }
  };

  // Appends a new tier to chart
  var addNewTier = function(args) {
    var actions = args.actions;
    var vmListView = args.vmListView;
    var actionPreFilter = args.actionPreFilter;
    var context = args.context;
    var tier = $.extend(args.tier, {
      context: context,
      vmListView: vmListView,
      actions: actions,
      actionPreFilter: actionPreFilter,
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
    var actions = args.actions;
    var context = args.context;
    var vmListView = args.vmListView;
    var actionPreFilter = args.actionPreFilter;
    var $tiers = args.$tiers;

    cloudStack.dialog.createForm({
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
                    context: $.extend(true, {}, context, {
                      tiers: [tier]
                    }),
                    tier: tier,
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
							cloudStack.dialog.notice({ message: _s(errorMsg) });							
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
    var siteToSiteVPN = args.siteToSiteVPN;

    return function(args) {
      var context = args.context;
      var $browser = $('#browser .container');
      var $toolbar = $('<div>').addClass('toolbar');
      var vpc = args.context.vpc[0];

      $browser.cloudBrowser('addPanel', {
        maximizeIfSelected: true,
        title: 'Configure VPC: ' + vpc.name,
        complete: function($panel) {
          var $loading = $('<div>').addClass('loading-overlay').appendTo($panel);

          $panel.append($toolbar);

          // Load data
          tierArgs.dataProvider({
            context: context,
            response: {
              success: function(args) {
                var tiers = args.tiers;
                var $chart = elems.chart({
                  $browser: $browser,
                  siteToSiteVPN: siteToSiteVPN,
                  vmListView: vmListView,
                  context: context,
                  actions: tierArgs.actions,
                  actionPreFilter: tierArgs.actionPreFilter,
                  vpcName: vpc.name,
                  tiers: tiers
                }).appendTo($panel);

                $loading.remove();
                $chart.fadeIn(function() {
                });
              }
            }
          });
        }
      });
    };
  };
}(jQuery, cloudStack));
