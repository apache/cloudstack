(function(cloudStack) {
  cloudStack.sections['global-settings'] = {
    title: 'label.menu.global.settings',
    id: 'global-settings',
    listView: {
      label: 'label.menu.global.settings',
      actions: {
        edit: {
          label: 'label.change.value',
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
                cloudStack.dialog.notice({ message: _l('message.restart.mgmt.server') });
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
        name: { label: 'label.name', id: true },
        description: { label: 'label.description' },
        value: { label: 'label.value', editable: true }
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
