(function(cloudStack, testData) {
  // Admin dashboard
  cloudStack.sections.dashboard = {
    title: 'Dashboard',
    show: cloudStack.uiCustom.dashboard,

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
