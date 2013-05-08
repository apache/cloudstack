(function($, cloudStack) {
  var elems = {
    tier: function(args) {
      var tier = args.tier;
      var dashboardItems = args.dashboardItems;
      var $tier = $('<div>').addClass('tier-item');
      var $header = $('<div>').addClass('header');
      var $title = $('<div>').addClass('title').append($('<span>'));
      var $content = $('<div>').addClass('content');
      var $dashboard = elems.dashboard({
        dashboardItems: dashboardItems
      });
      var $detailLink = $('<div>').addClass('detail-link');
      var $info = $('<div>').addClass('info');
      var $cidrLabel = $('<span>').addClass('cidr-label');
      var $cidr = $('<span>').addClass('cidr');

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
        tier: {
          name: 'Router',
        },
        dashboardItems: args.dashboardItems
      }).addClass('router');
      
      $router.find('.info, .detail-link').remove();
      $router.find('.header').prepend($('<span></span>').addClass('icon').html('&nbsp;'));
      
      return $router;
    },

    tierPlaceholder: function() {
      var $placeholder = $('<div>').addClass('tier-placeholder');

      $placeholder.append($('<span>').append('Create network'));
      
      return $placeholder;
    },

    dashboard: function(args) {
      var $dashboard = $('<div>').addClass('dashboard');

      $(args.dashboardItems).map(function(index, dashboardItem) {
        var $dashboardItem = $('<div>').addClass('dashboard-item');
        var $name = $('<div>').addClass('name').append($('<span>'));
        var $total = $('<div>').addClass('total').append($('<span>'));

        $name.find('span').html(dashboardItem.name);
        $total.find('span').html(dashboardItem.total);
        $dashboardItem.append($total, $name);
        $dashboardItem.appendTo($dashboard);
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
      var $chart = $('<div>').addClass('vpc-network-chart');
      var $tiers = $('<div>').addClass('tiers');

      $tiers.appendTo($chart);
      
      // Get tiers
      vpc.tiers.dataProvider({
        context: context,
        response: {
          success: function(data) {
            var tiers = data.tiers;

            $(tiers).map(function(index, tier) {
              var $tier = elems.tier({
                tier: tier,
                dashboardItems: [
                  {
                    name: 'Load balancers',
                    total: 5
                  },
                  {
                    name: 'Port forwarders',
                    total: 4
                  },
                  {
                    name: 'Static NATs',
                    total: 3
                  },
                  {
                    name: 'Virtual Machines',
                    total: 300
                  }
                ]
              });

              $tier.appendTo($tiers);
            });

            // Add placeholder tier
            $tiers.append(elems.tierPlaceholder());
          }
        }
      });

      // Router
      $router = elems.router({
        dashboardItems: [
          {
            name: 'Private gateways',
            total: 1
          },
          {
            name: 'Public IP addresses',
            total: 2
          },
          {
            name: 'Site-to-site VPNs',
            total: 3
          },
          {
            name: 'Network ACL lists',
            total: 2
          }
        ]
      }).appendTo($chart);
      
      $('#browser .container').cloudBrowser('addPanel', {
        title: vpcItem.displaytext ? vpcItem.displaytext : vpcItem.name,
        maximizeIfSelected: true,
        complete: function($panel) {
          $chart.appendTo($panel);
        }
      });
    };

    listConfigureAction.custom = vpcChart;
    detailsConfigureAction.custom = vpcChart;
  };
}(jQuery, cloudStack));
