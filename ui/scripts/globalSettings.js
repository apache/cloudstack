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
            debugger;
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
                args.response.success({data: item});
              },
              error: function(json) {                
                args.response.error({
                  message: $.parseJSON(json.responseText).updateconfigurationresponse.errortext
                });
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
        $.ajax({
          url: createURL("listConfigurations&page=" + args.page + "&pagesize=" + pageSize),
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
