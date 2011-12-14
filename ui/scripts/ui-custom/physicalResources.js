(function(cloudStack, $) {
  cloudStack.uiCustom.physicalResources = function(args) {
    var listView = function() {
      return $('<div>').listView(args);
    };

    var $dashboard = $('#template').find('.system-dashboard-view').clone();

    var getData = function() {
      // Populate data
      $dashboard.find('[data-item]').hide();
      cloudStack.sections.system.dashboard.dataProvider({
        response: {
          success: function(args) {
            var data = args.data;

            $.each(data, function(key, value) {
              var $elem = $dashboard.find('[data-item=' + key + ']');
              $elem.hide().html(value).fadeIn();
            });
          }
        }
      });
    };

    var resourceChart = function(args) {
      getData();
      
      return $dashboard
        .click(function(event) {
          var $target = $(event.target);

          if ($target.closest('.view-more').size()) {
            args.$browser.cloudBrowser('addPanel', {
              title: 'Zones',
              data: '',
              noSelectPanel: true,
              maximizeIfSelected: true,
              complete: function($newPanel) {
                listView().appendTo($newPanel);
              }
            });
          }
        });
    };

    $(window).bind('cloudStack.fullRefresh cloudStack.updateResources', function() {
      getData();
    });

    $dashboard.find('.button.refresh').click(function() {
      getData();
      
      return false;
    });

    return function(args) {
      return resourceChart(args);
    };
  };
}(cloudStack, jQuery));