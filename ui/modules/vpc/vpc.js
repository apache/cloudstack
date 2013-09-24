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
  var addTierDialog = function(args) {
    var $placeholder = args.$placeholder;
    var context = args.context;
    var addAction = cloudStack.vpc.tiers.actions.add;

    cloudStack.dialog.createForm({
      context: context,
      form: addAction.createForm,
      after: function(args) {
        var $loading = $('<div>').addClass('loading-overlay')
          .prependTo($placeholder);

        addAction.action({
          context: context,
          data: args.data,
          $form: args.$form,
          response: {
            success: function(args) {
              cloudStack.ui.notifications.add(
                // Notification
                {
                  desc: addAction.label
                },

                // Success
                function(args) {
                  $loading.remove();
                  $placeholder.closest('.vpc-network-chart').trigger('reload');
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

  var elems = {
    tier: function(args) {
      var tier = args.tier;
      var context = $.extend(true, {}, args.context, {
        networks: [tier]
      });
      var dashboardItems = args.dashboardItems;
      var $tier = $('<div>').addClass('tier-item');
      var $header = $('<div>').addClass('header');
      var $title = $('<div>').addClass('title').append($('<span>'));
      var $content = $('<div>').addClass('content');
      var $browser = $('#browser .container');
      var $dashboard = elems.dashboard({
        context: context,
        dashboardItems: dashboardItems
      });
      var $detailLink = $('<div>').addClass('detail-link');
      var $info = $('<div>').addClass('info');
      var $cidrLabel = $('<span>').addClass('cidr-label');
      var $cidr = $('<span>').addClass('cidr');

      $detailLink.click(function() {
        $browser.cloudBrowser('addPanel', {
          title: tier.displayname ? tier.displayname : tier.name,
          complete: function($panel) {
            var $detailView = $('<div>').detailView(
              $.extend(true, {}, cloudStack.vpc.tiers.detailView, {
                section: 'networks',
                $browser: $browser,
                context: context,
                onActionComplete: function() {
                  $tier.closest('.vpc-network-chart').trigger('reload');
                }
              })
            );

            $detailView.appendTo($panel);
          }
        });
      });

      $cidrLabel.html('CIDR: ');
      $cidr.html(tier.cidr);
      $title.find('span').html(tier.displayname ? tier.displayname : tier.name);
      $header.append($title, $detailLink);
      $info.append($cidrLabel, $cidr);
      $content.append($dashboard, $info);
      $tier.append($header, $content);

      return $tier;
    },

    connectorLine: function(args) {
      var $connector = $('<div></div>').addClass('connector-line');
      var $router = args.$router;
      var $tier = args.$tier;
      var isHighlighted = args.isHighlighted;
      var $connectorStart = $('<div></div>').addClass('connector-start');
      var $connectorMid = $('<div></div>').addClass('connector-mid');
      var $connectorEnd = $('<div></div>').addClass('connector-end');

      $connector.append($connectorStart, $connectorMid, $connectorEnd);

      if (isHighlighted) {
        $connector.addClass('highlighted');
      }

      var posStartOffsetLeft = 5;
      var posStartOffsetTop = 10;
      var posStart = {
        top: $router.position().top + ($router.outerHeight() / 2 + ($tier.index() * posStartOffsetTop)),
        left: $router.position().left + $router.outerWidth()
      };
      var posStartWidth = 60 - ($tier.index() > 2 ? (($tier.index() + 1) * posStartOffsetLeft) : 0);

      var posEndOffset = 15;
      var posEndWidthOffset = 3;
      var posEnd = {
        top: $tier.position().top + ($tier.outerHeight() / 2),
        left: posStart.left + posStartWidth + posEndOffset
      };
      var posEndWidth = Math.abs($tier.position().left -
                                 (posStart.left + posStartWidth)) + posEndWidthOffset;

      // Start line (next to router)
      $connectorStart.css({
        top: posStart.top,
        left: posStart.left
      });
      $connectorStart.width(posStartWidth);

      // End line (next to tier)
      $connectorEnd.css({
        top: posEnd.top,
        left: posEnd.left
      });
      $connectorEnd.width(posEndWidth);

      // Mid line (connect start->end)
      if (posStart.top > posEnd.top) { // Tier above router
        $connectorMid.css({
          top: posEnd.top,
          left: posEnd.left
        });
        $connectorMid.height(posStart.top - posEnd.top);
      } else { // Tier below router
        $connectorMid.css({
          top: posStart.top,
          left: posStart.left + posStartWidth + posEndOffset
        });
        $connectorMid.height(posEnd.top - posStart.top);
      }

      return $connector;
    },

    router: function(args) {
      var $router = elems.tier({
        context: args.context,
        tier: {
          name: 'Router'
        },
        dashboardItems: args.dashboardItems
      }).addClass('router');

      $router.find('.info, .detail-link').remove();
      $router.find('.header').prepend($('<span></span>').addClass('icon').html('&nbsp;'));

      return $router;
    },

    tierPlaceholder: function(args) {
      var context = args.context;
      var $placeholder = $('<div>').addClass('tier-placeholder');

      $placeholder.append($('<span>').append('Create network'));
      $placeholder.click(function() {
        addTierDialog({
          context: context,
          $placeholder: $placeholder
        });
      });

      return $placeholder;
    },

    dashboard: function(args) {
      var $dashboard = $('<div>').addClass('dashboard');
      var context = args.context;
      var tier = context.networks[0];

      $(args.dashboardItems).map(function(index, dashboardItem) {
        var $dashboardItem = $('<div>').addClass('dashboard-item');
        var $name = $('<div>').addClass('name').append($('<span>'));
        var $total = $('<div>').addClass('total').append($('<span>'));
        var id = dashboardItem.id;

        $name.find('span').html(dashboardItem.name);


        if (dashboardItem.totalMultiLine) {
          $total.find('span').html(dashboardItem.totalMultiLine);
          $total.addClass('multiline');
        } else {
          $total.find('span').html(dashboardItem.total ? dashboardItem.total : 0);
        }

        $dashboardItem.append($total, $name);
        $dashboardItem.appendTo($dashboard);

        if (dashboardItem._disabled) {
          $dashboardItem.addClass('disabled');
        }

        $dashboardItem.click(function() {
          if ($dashboardItem.is('.disabled')) {
            return false;
          }
          
          var section = cloudStack.vpc.sections[id];
          var $section = $('<div>');
          var $loading = $('<div>').addClass('loading-overlay');

          if ($.isFunction(section)) {
            section = cloudStack.vpc.sections[id]();
          }

          var before = section.before;
          var load = function() {
            $('#browser .container').cloudBrowser('addPanel', {
              title: tier.name + ' - ' + dashboardItem.name,
              maximizeIfSelected: true,
              complete: function($panel) {
                if (section.listView) {
                  $section.listView($.extend(true, {}, section, {
                    onActionComplete: function() {
                      $dashboardItem.closest('.vpc-network-chart').trigger('reload');
                    },
                    context: context
                  }));
                }

                $section.appendTo($panel);
              }
            });
          };

          if (before) {
            before.check({
              context: context,
              response: {
                success: function(result) {
                  // true means content exists
                  if (result) {
                    load();
                  } else {
                    cloudStack.dialog.confirm({
                      message: before.messages.confirm,
                      action: function() {
                        $loading.appendTo($dashboardItem.closest('.vpc-network-chart'));
                        before.action({
                          context: context,
                          response: {
                            success: function() {
                              $loading.remove();
                              $dashboardItem.closest('.vpc-network-chart').trigger('reload');
                              load();
                            }
                          }
                        });
                      }
                    })
                  }
                }
              }
            });
          } else {
            load();
          }
        });
      });

      return $dashboard;
    }
  };

  cloudStack.modules.vpc = function(module) {
    var vpc = cloudStack.vpc;
    var vpcSection = cloudStack.sections.network.sections.vpc;
    var listConfigureAction = vpcSection.listView.actions.configureVpc.action;
    var detailsConfigureAction = vpcSection.listView.detailView.actions.configureVpc.action;

    var vpcChart = function(args) {
      var context = args.context;
      var vpcItem = context.vpc[0];

      var chart = function(args) {
        args = args ? args : {};

        var $chart = $('<div>').addClass('vpc-network-chart');
        var $tiers = $('<div>').addClass('tiers');
        var $toolbar = $('<div>').addClass('toolbar');
        var $info = $('<div>').addClass('info-box');

        $toolbar.appendTo($chart);
        $tiers.appendTo($chart);

        // Get tiers
        var $loading = $('<div>').addClass('loading-overlay').prependTo($chart);
        vpc.tiers.dataProvider({
          context: context,
          response: {
            success: function(data) {
              var tiers = data.tiers;
              var $router;
              var $placeholder = elems.tierPlaceholder({
                context: context
              });

              // Router
              $router = elems.router({
                context: context,
                dashboardItems: data.routerDashboard
              }).appendTo($chart);

              $(tiers).map(function(index, tier) {
                var $tier = elems.tier({
                  context: context,
                  tier: tier,
                  dashboardItems: tier._dashboardItems
                });
                $tier.appendTo($tiers);

                // Connect tier to router via line
                //
                // -- Needs to execute after chart generation is complete,
                //    so that chart elements have positioning in place.
                $chart.bind('cloudStack.vpc.chartReady', function() {
                  elems.connectorLine({
                    $tier: $tier,
                    $router: $router,
                    isHighlighted: tier._highlighted
                  }).appendTo($chart);
                });
              });

              // Add placeholder tier
              $tiers.append($placeholder);
              $loading.remove();

              if (!tiers.length) {
                addTierDialog({
                  context: context,
                  $placeholder: $placeholder
                });
              }

              if (args.complete) {
                args.complete($chart);
              }

              if ($chart.find('.connector-line.highlighted').size()) {
                $info.appendTo($chart).append(
                  $('<span>').addClass('color-key'),
                  $('<span>').html('= Contains a public network')
                );
              }
            }
          }
        });

        $chart.bind('reload', function() {
          chart({
            complete: function($newChart) {
              $chart.replaceWith($newChart);
              $newChart.trigger('cloudStack.vpc.chartReady');
            }
          });
        });

        return $chart;
      };

      $('#browser .container').cloudBrowser('addPanel', {
        title: vpcItem.displaytext ? vpcItem.displaytext : vpcItem.name,
        maximizeIfSelected: true,
        complete: function($panel) {
          var $chart = chart({
            complete: function($chart) {
              $chart.trigger('cloudStack.vpc.chartReady');
            }
          });

          $chart.appendTo($panel);
        }
      });
    };

    listConfigureAction.custom = vpcChart;
    detailsConfigureAction.custom = vpcChart;
  };
}(jQuery, cloudStack));
