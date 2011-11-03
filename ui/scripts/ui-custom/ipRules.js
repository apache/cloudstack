(function($, cloudStack) {
  cloudStack.ipRules = function(args) {
    return function(detailArgs) {
      var context = detailArgs.context;
      
      var portMultiEdit = function(args) {
        return $('<div>').multiEdit(args);
      };

      var makeMultiEditPanel = function($item, options) {
        if (!options) options = {};
        if ($item.closest('li').hasClass('disabled')) return false;
        
        var targetId = $item.attr('net-target');
        var targetName = $item.parent().find('.name').find('span').html();
        var target = args[targetId];

        var $browser = $item.closest('.detail-view').data('view-args').$browser;

        $browser.cloudBrowser('addPanel', {
          title: options.title ? options.title : targetName,
          maximizeIfSelected: true,
          complete: function($newPanel) {
            $newPanel.detailView({
              $browser: $browser,
              name: targetId,
              context: context,
              tabs: {
                network: {
                  title: options.title ? options.title : targetName,
                  custom: function(args) {
                    return portMultiEdit($.extend(target, {
                      context: context
                    }));
                  }
                }
              }
            });
          }
        });

        return true;
      };

      var staticNATChart = function(args) {
        var $chart = $('#template').find('.network-chart.static-nat').clone();

        $chart.find('li.firewall .view-details').click(function() {
          makeMultiEditPanel($(this), { title: 'NAT Port Range'});
        });
        
        return $chart;
      };

      var netChart = function(args) {
        var $chart = $('#template').find('.network-chart.normal').clone();
        var preFilter = args.preFilter ? args.preFilter({
          items: ['firewall', 'portForwarding', 'loadBalancing'],
          context: context
        }) : [];

        // Filter disabled tabs
        if (preFilter.length) {
          if (preFilter.length == 3) { // All items
            // Assume this is a static NAT
            return staticNATChart(args);
          } else {
            $(preFilter).each(function() {
              var id = this;
              
              var $li = $chart.find('li').filter(function() {
                return $(this).hasClass(id);
              }).addClass('disabled');
            });            
          }
        }

        $chart.find('.view-details').click(function() {
          makeMultiEditPanel($(this));
          return false;
        });

        return $chart;
      };

      return netChart(args);
    };
  };
})(jQuery, cloudStack);
