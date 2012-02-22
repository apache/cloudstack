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
        var $vmName = $chart.find('li.static-nat-enabled .vmname');
        var $browser = $('#browser .container');
        var vmDataProvider = args.vmDataProvider;
        var vmDetails = args.vmDetails;

        args.staticNATDataProvider({
          context: context,
          response: {
            success: function(args) {
              var vmID = args.data.virtualmachineid;
              var vmName = args.data.virtualmachinename;

              $vmName.append(
                $('<span>').html('VM: ' + vmName)
              );

              $vmName.click(function() {
                $browser.cloudBrowser('addPanel', {
                  title: _l('label.static.nat.vm.details'),
                  complete: function($newPanel) {
                    vmDataProvider({
                      context: context,
                      response: {
                        success: function(args) {
                          var instance = args.data;
                          var detailViewArgs = $.extend(true, {}, vmDetails, {
                            $browser: $browser,
                            context: $.extend(true, {}, context, {
                              instances: [instance]
                            }),
                            jsonObj: instance,
                            id: instance.id
                          });

                          // No actions available
                          detailViewArgs.actions = {};

                          $newPanel.detailView(detailViewArgs);
                        }
                      }
                    });
                  }
                });
              });
            }
          }
        });
							
				$chart.find('li.firewall .view-details').click(function() {
					makeMultiEditPanel($(this), { title: _l('label.nat.port.range')});
				});					

        return $chart;
      };

      var netChart = function(args) {
		
        var $chart = $('#template').find('.network-chart.normal').clone();
        var preFilter = args.preFilter ? args.preFilter({
          items: ['firewall', 'portForwarding', 'loadBalancing'],
          context: context
        }) : [];

        // 1. choose between staticNAT chart and non-staticNAT chart  2. filter disabled tabs  
        if (preFilter.length) {				 
					if($.inArray('nonStaticNATChart', preFilter) != -1) { //choose static NAT chart					
					  return staticNATChart(args); 		
					}
					else {  //choose non-static NAT chart
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
