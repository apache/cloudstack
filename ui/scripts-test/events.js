(function(cloudStack) {
  cloudStack.sections.events = {
    title: 'Events',
    id: 'events',
    sectionSelect: {
      preFilter: function(args) {
        var user = args.context.users[0];

        if (user.role == 'admin')
          return args.context.sections;

        return ['events'];
      },
      label: 'Select view'
    },
    sections: {
      events: {
        type: 'select',
        title: 'Events',
        listView: {
          id: 'events',
          label: 'Events',
          fields: {
            type: { label: 'Type' },
            description: { label: 'Description' },
            username: { label: 'Initiated By' },
            created: { label: 'Date' }
          },
          dataProvider: testData.dataProvider.listView('events')
        }
      },
      alerts: {
        type: 'select',
        title: 'Alerts',
        listView: {
          id: 'alerts',
          label: 'Alerts',
          fields: {
            type: { label: 'Type' },
            description: { label: 'Description' },
            sent: { label: 'Date' }
          },
          dataProvider: testData.dataProvider.listView('alerts'),
          detailView: {
            name: 'Alert details',
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    type: { label: 'Type' },
                    description: { label: 'Description' },
                    created: { label: 'Sent' }
                  }
                ],
                dataProvider: testData.dataProvider.detailView('alerts')
              }
            }
          }
        }
      }
    }
  };
})(cloudStack);
