(function(cloudStack) {
  cloudStack.sections['global-settings'] = {
    title: 'Global Settings',
    id: 'global-settings',
    listView: {
      label: 'Global Settings',
      actions: {
        edit: {
          label: 'Change value',
          action: function(args) {           
            var name = args.data.jsonObj.name;
            var value = args.data.value;

            $.ajax({
              url: createURL(
                'updateConfiguration&name=' + name + '&value=' + value
              ),
              dataType: 'json',
              async: true,
              success: function(json) {                
                var item = json.updateconfigurationresponse.configuration;
                cloudStack.dialog.notice({ message: 'Please restart your management server for your change to take effect.' });
                args.response.success({data: item});
              },
              error: function(json) {                
                args.response.error(parseXMLHttpResponse(json));
              }
            });
          }
        }
      },
      fields: {
        name: { label: 'Name', id: true },
        description: { label: 'Description' },
        value: { label: 'Value', editable: true }
      },
      dataProvider: function(args) {
        var data = {
          page: args.page,
          pagesize: pageSize
        };

        if (args.filterBy.search.value) {
          data.name = args.filterBy.search.value;
        }

        $.ajax({
          url: createURL('listConfigurations'),
          data: data,
          dataType: "json",
          async: true,
          success: function(json) {
            var items = json.listconfigurationsresponse.configuration;
            args.response.success({ data: items });
          }
        });
      }
    }
  };
})(cloudStack);
