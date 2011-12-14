(function(cloudStack, testData) {
  // Admin dashboard
  cloudStack.sections.dashboard = {
    title: 'Dashboard',
    show: function() {
      var $dashboard = $('#template').find('div.dashboard.admin').clone();

      $dashboard.find('.view-all').click(function() {
        $('#navigation li.events').click();
      });

      var getData = function() {
        // Populate data
        $dashboard.find('[data-item]').hide();
        cloudStack.sections.dashboard.dataProvider({
          response: {
            success: function(args) {
              var data = args.data;

              $.each(data, function(key, value) {
                var $elem = $dashboard.find('[data-item=' + key + ']');

                $elem.each(function() {
                  var $item = $(this);
                  if ($item.hasClass('chart-line')) {
                    $item.show().animate({ width: value + '%' });
                  } else {
                    $item.hide().html(value).fadeIn();
                  }
                });
              });
            }
          }
        });
      };

      getData();

      return $dashboard;
    },

    dataProvider: function(args) {
      args.response.success({
        data: {
          publicIPAllocated: 50,
          publicIPTotal: 100,
          publicIPPercentage: 50,
          privateIPAllocated: 50,
          privateIPTotal: 100,
          privateIPPercentage: (100 / 50) * 10,
          memoryAllocated: 256,
          memoryTotal: 1024,
          memoryPercentage: (1024 / 256) * 10,
          cpuAllocated: 500,
          cpuTotal: 1200,
          cpuPercentage: (1200 / 500) * 10
        }
      });
    }
  };
})(cloudStack, testData);
