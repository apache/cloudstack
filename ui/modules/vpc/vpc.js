(function($, cloudStack) {
  var elems = {
    chart: {
      tier: function(args) {
        var tier = args.tier;
        var $tier = $('<div>').addClass('tier-item');
        var $header = $('<div>').addClass('header');
        var $title = $('<div>').addClass('title').append($('<span>'));
        var $content = $('<div>').addClass('content');
        var $dashboard = $('<div>').addClass('dashboard');
        var $info = $('<div>').addClass('info');
        var $cidrLabel = $('<span>').addClass('cidr-label');
        var $cidr = $('<span>').addClass('cidr');

        $cidrLabel.html('CIDR: ');
        $cidr.html(tier.cidr);
        $title.find('span').html(tier.displayname ? tier.displayname : tier.name);
        $header.append($title);
        $info.append($cidrLabel, $cidr);
        $content.append($dashboard, $info);
        $tier.append($header, $content);

        return $tier;
      }
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
              var $tier = elems.chart.tier({ tier: tier });

              $tier.appendTo($tiers);
            });
          }
        }
      });
      
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
