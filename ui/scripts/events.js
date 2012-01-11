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
            created: { label: 'Date', converter: cloudStack.converters.toLocalDate }
          },
          dataProvider: function(args) {					  
						var array1 = [];  
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
						
            $.ajax({
              url: createURL("listEvents&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
              dataType: "json",
              async: true,
              success: function(json) {
                var items = json.listeventsresponse.event;
                args.response.success({data:items});
              }
            });
          },
					detailView: {
            name: 'Event details',
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    type: { label: 'Type' },
                    description: { label: 'Description' },
                    created: { label: 'Date', converter: cloudStack.converters.toLocalDate }
                  }
                ],
                dataProvider: function(args) {
								  args.response.success({data: args.context.events[0]});
								}
              }
            }
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
            description: { label: 'Description' },
            sent: { label: 'Date', converter: cloudStack.converters.toLocalDate }
          },
          dataProvider: function(args) {
					  var array1 = [];  
						if(args.filterBy != null) {          
							if(args.filterBy.search != null && args.filterBy.search.by != null && args.filterBy.search.value != null) {
								switch(args.filterBy.search.by) {
								case "name":
									if(args.filterBy.search.value.length > 0)
										array1.push("&keyword=" + args.filterBy.search.value);
									break;
								}
							}
						}
            $.ajax({
              url: createURL("listAlerts&listAll=true&page=" + args.page + "&pagesize=" + pageSize + array1.join("")),
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
                    id: { label: 'ID' },
                    description: { label: 'Description' },
                    sent: { label: 'Date', converter: cloudStack.converters.toLocalDate }
                  }
                ],
                dataProvider: function(args) {
								  args.response.success({data: args.context.alerts[0]});
								}
              }
            }
          }
        }
      }
    }
  };
})(cloudStack, testData);
