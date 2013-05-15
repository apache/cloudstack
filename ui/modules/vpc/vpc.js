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

    router: function(args) {
      var $router = elems.tier({
        context: args.context,
        tier: {
          name: 'Router',
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
        var addAction = cloudStack.vpc.tiers.actions.add;
        var form = cloudStack.dialog.createForm({
          context: context,
          form: addAction.createForm,
          after: function(args) {
            var $loading = $('<div>').addClass('loading-overlay')
              .prependTo($placeholder);

            addAction.action({
              context: context,
              data: args.data,
              response: {
                success: function(args) {
                  var tier = args.data;

                  cloudStack.ui.notifications.add(
                    // Notification
                    {
                      desc: addAction.label
                    },

                    // Success
                    function(args) {
                      $loading.remove();
                      $placeholder.closest('.vpc-network-chart').trigger('reload');
                      
                      // addNewTier({
                      //   ipAddresses: ipAddresses,
                      //   $browser: $browser,
                      //   tierDetailView: tierDetailView,
                      //   context: $.extend(true, {}, context, {
                      //     networks: [tier]
                      //   }),
                      //   tier: tier,
                      //   acl: acl,
                      //   $tiers: $tiers,
                      //   actions: actions,
                      //   actionPreFilter: actionPreFilter,
                      //   vmListView: vmListView
                      // });
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
        $total.find('span').html(dashboardItem.total);
        $dashboardItem.append($total, $name);
        $dashboardItem.appendTo($dashboard);

        $dashboardItem.click(function() {
          $('#browser .container').cloudBrowser('addPanel', {
            title: tier.name + ' - ' + dashboardItem.name,
            maximizeIfSelected: true,
            complete: function($panel) {
              var section = cloudStack.vpc.sections[id];
              var $section = $('<div>');

              if ($.isFunction(section)) {
                section = cloudStack.vpc.sections[id]()
              }

              if (section.listView) {
                $section.listView($.extend(true, {}, section, {
                  context: context
                }));
              }

              $section.appendTo($panel);
            }
          });
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

        $toolbar.appendTo($chart);
        $tiers.appendTo($chart);

        // Get tiers
        var $loading = $('<div>').addClass('loading-overlay').prependTo($chart);
        vpc.tiers.dataProvider({
          context: context,
          response: {
            success: function(data) {
              var tiers = data.tiers;

              $(tiers).map(function(index, tier) {
                var $tier = elems.tier({
                  context: context,
                  tier: tier,
                  dashboardItems: [
                    {
                      id: 'tierLoadBalancers',
                      name: 'Load balancers',
                      total: 5
                    },
                    {
                      id: 'tierPortForwarders',
                      name: 'Port forwarders',
                      total: 4
                    },
                    {
                      id: 'tierStaticNATs',
                      name: 'Static NATs',
                      total: 3
                    },
                    {
                      id: 'tierVMs',
                      name: 'Virtual Machines',
                      total: 300
                    }
                  ]
                });

                $tier.appendTo($tiers);
              });

              // Add placeholder tier
              $tiers.append(elems.tierPlaceholder({
                context: context
              }));

              $loading.remove();
              
              if (args.complete) {
                args.complete($chart);
              }
            }
          }
        });

        // Router
        $router = elems.router({
          context: context,
          dashboardItems: [
            {
              id: 'privateGateways',
              name: 'Private gateways',
              total: 1
            },
            {
              id: 'publicIPs',
              name: 'Public IP addresses',
              total: 2
            },
            {
              id: 'siteToSiteVPNs',
              name: 'Site-to-site VPNs',
              total: 3
            },
            {
              id: 'networkACLLists',
              name: 'Network ACL lists',
              total: 2
            }
          ]
        }).appendTo($chart);

        $chart.bind('reload', function() {
          chart({
            complete: function($newChart) {
              $chart.replaceWith($newChart);              
            }
          });
        });

        return $chart;
      };

      $('#browser .container').cloudBrowser('addPanel', {
        title: vpcItem.displaytext ? vpcItem.displaytext : vpcItem.name,
        maximizeIfSelected: true,
        complete: function($panel) {
          var $chart = chart();

          $chart.appendTo($panel);
        }
      });
    };

    listConfigureAction.custom = vpcChart;
    detailsConfigureAction.custom = vpcChart;
  };
}(jQuery, cloudStack));
