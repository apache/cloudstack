(function(cloudStack, $) {
  cloudStack.uiCustom.physicalResources = function(args) {
    var listView = function() {
      return $('<div>').listView(args);
    };

    var resourceChart = function(args) {
      return $('#template').find('.system-dashboard-view').clone()
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
    
    return function(args) {
      return resourceChart(args);
    };
  };
}(cloudStack, jQuery));