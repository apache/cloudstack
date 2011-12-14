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
      var dataFns = {
        zones: function(data) {
          $.ajax({
            url: createURL('listZones'),
            success: function(json) {
              dataFns.capacity({
                zones: json.listzonesresponse.zone
              });
            }
          });
        },
        capacity: function(data) {
          if (data.zones) {
            $.ajax({
              url: createURL('listCapacity'),
              success: function(json) {
                var capacities = json.listcapacityresponse.capacity;

                var capacity = function(id, converter) {
                  return $.grep(capacities, function(capacity) {
                    return capacity.type == id;
                  })[0];
                };

                complete($.extend(data, {
                  publicIPAllocated: capacity(8).capacityused,
                  publicIPTotal: capacity(8).capacitytotal,
                  publicIPPercentage: parseInt(capacity(8).percentused),
                  privateIPAllocated: capacity(5).capacityused,
                  privateIPTotal: capacity(5).capacitytotal,
                  privateIPPercentage: parseInt(capacity(8).percentused),
                  memoryAllocated: cloudStack.converters.convertBytes(capacity(0).capacityused),
                  memoryTotal: cloudStack.converters.convertBytes(capacity(0).capacitytotal),
                  memoryPercentage: parseInt(capacity(0).percentused),
                  cpuAllocated: cloudStack.converters.convertHz(capacity(1).capacityused),
                  cpuTotal: cloudStack.converters.convertHz(capacity(1).capacitytotal),
                  cpuPercentage: parseInt(capacity(1).percentused)
                }));
              }
            });
          } else {
            complete($.extend(data, {
              publicIPAllocated: 0,
              publicIPTotal: 0,
              publicIPPercentage: 0,
              privateIPAllocated: 0,
              privateIPTotal: 0,
              privateIPPercentage: 0,
              memoryAllocated: 0,
              memoryTotal: 0,
              memoryPercentage: 0,
              cpuAllocated: 0,
              cpuTotal: 0,
              cpuPercentage: 0
            }));
          }
        }
      };

      var complete = function(data) {
        args.response.success({
          data: data
        });
      };

      dataFns.zones({});
    }
  };
})(cloudStack, testData);
