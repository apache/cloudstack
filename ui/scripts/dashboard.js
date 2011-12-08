(function(cloudStack, testData) {
  // Admin dashboard
  cloudStack.sections.dashboard = {
    title: 'Dashboard',
    show: function() {
      var $dashboard = $('#template').find('div.dashboard.admin').clone();

      $dashboard.find('.view-all').click(function() {
        $('#navigation li.events').click();
      });

      return $dashboard;
    }
  };

  // User dashboard
  /*
  cloudStack.sections['dashboard-user'] = {
    title: 'Dashboard (user)',
    show: function() {
      return $('#template').find('div.dashboard.user').clone();
    }
  };
  */
  
})(cloudStack, testData);
