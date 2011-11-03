(function(cloudStack, testData) {
  // Admin dashboard
  cloudStack.sections.dashboard = {
    title: 'Dashboard',
    show: function() {
      return $('#template').find('div.dashboard.admin').clone();
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
