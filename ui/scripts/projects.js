(function(cloudStack, testData) {
  cloudStack.projects = {
    add: function(args) {
      setTimeout(function() {
        args.response.success({
          data: {
            name: args.data['project-name'],
            displayText: args.data['project-display-text'],
            users: []
          }
        });
      }, 1000);
    },
    addUserForm: {
      noSelect: true,
      fields: {
        'username': { edit: true, label: 'Account' },
        'role': {
          label: 'Role',
          select: function(args) {
            args.response.success({
              data: [
                { name: 'user', description: 'User' },
                { name: 'admin', description: 'Admin' }
              ]
            });
          }
        },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Invite',
        action: function(args) {
          setTimeout(function() {
            cloudStack.context.projects[0].users.push(args.data);
            args.response.success({
              notification: {
                label: 'Added user to project',
                poll: testData.notifications.testPoll
              }
            });
          }, 100);
        }
      },
      actionPreFilter: function(args) {
        if (cloudStack.context.projects &&
            cloudStack.context.projects[0] &&
            !cloudStack.context.projects[0].isNew) {
          return args.context.actions;
        }

        return ['destroy'];
      },
      actions: {
        destroy: {
          label: 'Remove user from project',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                notification: {
                  label: 'Removed user from project',
                  poll: testData.notifications.testPoll
                }
              });
            }, 500);
          }
        },

        makeOwner: {
          label: 'Make user project owner',
          action: function(args) {
            setTimeout(function() {
              args.response.success({
                notification: {
                  label: 'Assigned new owner to project',
                  poll: testData.notifications.testPoll
                }
              });
            });
          }
        }
      },

      // Project users data provider
      dataProvider: function(args) {
        var data = cloudStack.context.projects ?
              cloudStack.context.projects[0].users : [];

        setTimeout(function() {
          args.response.success({
            data: data
          });
        }, 100);
      }
    },

    // Project listing data provider
    dataProvider: function(args) {
      var user = args.context.users[0];
      setTimeout(function() {
        args.response.success({
          data: user.username == 'bfederle' ? [] : testData.data.projects
        });
      }, 200);
    }
  };
} (cloudStack, testData));