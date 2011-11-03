(function(cloudStack, testData) {
  cloudStack.sections.events = {
    title: 'Events',
    id: 'events',
    sectionSelect: {
      preFilter: function(args) {        
        if(isAdmin()) 
          return ["events", "alerts"];
        else
          return ["events"];      
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
          dataProvider: function(args) {            
            $.ajax({
              url: createURL("listEvents&page="+args.page+"&pagesize="+pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listeventsresponse.event;
                args.response.success({data:items});
              }
            });
          }
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
          dataProvider: function(args) {
            $.ajax({
              url: createURL("listAlerts&page="+args.page+"&pagesize="+pageSize),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listalertsresponse.alert;
                args.response.success({data:items});
              }
            });
          },
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
              },
            }
          }
        }
      }
    }
  };
})(cloudStack, testData);
