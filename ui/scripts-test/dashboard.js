(function(cloudStack, testData) {
  // Admin dashboard
  cloudStack.sections.dashboard = {
    title: 'Dashboard',
    show: cloudStack.uiCustom.dashboard,

    adminCheck: function(args) {
      return args.context.users[0].role == 'admin';
    },

    user: {
      dataProvider: function(args) {
        args.response.success({
          data: {
            runningInstances: 10,
            stoppedInstances: 2,
            totalInstances: 12,
            accountID: '12',
            accountName: 'brian',
            userName: 'brian',
            accountType: 'user',
            accountDomainID: 12
          }
        })
      }
    },

    admin: {
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
            cpuPercentage: (1200 / 500) * 10,
            alerts: $.map(testData.data.alerts, function(alert) {
              return {
                name: 'System Alert',
                description: alert.description
              };
            }),
            hostAlerts: $.map(testData.data.alerts, function(alert) {
              return {
                name: 'Host Alert',
                description: alert.description
              };
            })
          }
        });
      }
    }
  };
})(cloudStack, testData);
