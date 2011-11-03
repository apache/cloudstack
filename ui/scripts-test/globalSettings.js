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
            args.response.success();
          },
          notification: {
            poll: testData.notifications.testPoll
          }
        }
      },
      fields: {
        name: { label: 'Name', id: true },
        description: { label: 'Description' },
        value: { label: 'Value', editable: true }
      },
      dataProvider: testData.dataProvider.listView('globalSettings')
    }
  };
})(cloudStack);
