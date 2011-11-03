(function(cloudStack) {
  cloudStack.sections.templates = {
    title: 'Templates',
    id: 'templates',
    sectionSelect: {
      label: 'Select view'
    },
    sections: {
      templates: {
        type: 'select',
        title: 'Templates',
        listView: {
          id: 'templates',
          label: 'Templates',
          fields: {
            displaytext: { label: 'Name', editable: true },
            desc: { label: 'Details' },
            zonename: { label: 'Zone' },
            hypervisor: { label: 'Hypervisor' }
          },
          actions: {
            // Add template
            add: {
              label: 'Add template',

              action: function(args) {
                args.response.success();
              },

              messages: {
                notification: function(args) {
                  return 'Created template';
                }
              },

              createForm: {
                title: 'Add new template',
                desc: 'Please enter the following data to create your new template',

                fields: {
                  name: { label: 'Name', validation: { required: true } },
                  displayText: { label: 'Display Text', validation: { required: true } },
                  url: { label: 'URL', validation: { required: true } },
                  passwordEnabled: { label: 'Password', isBoolean: true }
                }
              },

              notification: {
                poll: testData.notifications.testPoll
              }
            },
            edit: {
              label: 'Edit template name',
              action: function(args) {
                args.response.success(args.data[0]);
              }
            }
          },
          dataProvider: testData.dataProvider.listView('templates')
        }
      },
      isos: {
        type: 'select',
        title: 'ISOs',
        listView: {
          label: 'ISOs',
          fields: {
            displaytext: { label: 'Name' },
            desc: { label: 'Details' },
            size: { label: 'Size' },
            zonename: { label: 'Zone' }
          },
          dataProvider: testData.dataProvider.listView('isos')
        }
      }
    }
  };  
})(cloudStack);
