(function(cloudStack) {
  cloudStack.sections.events = {
    title: 'label.menu.events',
    id: 'events',
    sectionSelect: {
      preFilter: function(args) {
        if(isAdmin())
          return ["events", "alerts"];
        else
          return ["events"];
      },
      label: 'label.select-view'
    },
    sections: {
      events: {
        type: 'select',
        title: 'label.menu.events',
        listView: {
          id: 'events',
          label: 'label.menu.events',
          fields: {
            type: { label: 'label.type' },
            description: { label: 'label.description' },
            username: { label: 'label.initiated.by' },
            created: { label: 'label.date', converter: cloudStack.converters.toLocalDate }
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
            name: 'label.details',
            tabs: {
              details: {
                title: 'label.details',
                fields: [
                  {
                    type: { label: 'label.type' },
                    description: { label: 'label.description' },
                    created: { label: 'label.date', converter: cloudStack.converters.toLocalDate }
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
        title: 'label.menu.alerts',
        listView: {
          id: 'alerts',
          label: 'label.menu.alerts',
          fields: {
            description: { label: 'label.description' },
            sent: { label: 'label.date', converter: cloudStack.converters.toLocalDate }
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
                title: 'label.details',
                fields: [
                  {
                    id: { label: 'ID' },
                    description: { label: 'label.description' },
                    sent: { label: 'label.date', converter: cloudStack.converters.toLocalDate }
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
})(cloudStack);
