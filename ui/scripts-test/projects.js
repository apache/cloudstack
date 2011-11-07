(function($, cloudStack, testData) {
  cloudStack.projects = {
    requireInvitation: function(args) {
      return cloudStack.context.users[0].username == 'jdoe';
    },
    
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
    inviteForm: {
      noSelect: true,
      fields: {
        'email': { edit: true, label: 'E-mail' },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Invite',
        action: function(args) {
          setTimeout(function() {
            args.response.success({
              data: args.data,
              notification: {
                label: 'Invited user to project',
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

      actions: {},

      // Project users data provider
      dataProvider: function(args) {
        var data = cloudStack.context.projects ?
              [
                { email: 'brian.federle@citrix.com' },
                { email: 'john.doe@aol.com' },
                { email: 'some.user@gmail.com' }
              ] : [];

        setTimeout(function() {
          args.response.success({
            data: data
          });
        }, 100);
      }
    },
    addUserForm: {
      noSelect: true,
      fields: {
        'username': { edit: true, label: 'Account' },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Add user',
        action: function(args) {
          setTimeout(function() {
            args.response.success({
              data: args.data,
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

  /**
   * Projects section -- list view
   */
  cloudStack.sections.projects = {
    title: 'Projects',
    id: 'projects',
    listView: {
      fields: {
        name: { label: 'Project Name' },
        displayText: { label: 'Display Text' },
        domain: { label: 'Domain' },
        account: { label: 'Owner' }
      },

      dataProvider: testData.dataProvider.listView('projects'),

      actions: {
        add: {
          label: 'New Project',
          action: {
            custom: function(args) {
              $(window).trigger('cloudStack.newProject');
            }
          },

          messages: {
            confirm: function(args) {
              return 'Are you sure you want to remove ' + args.name + '?';
            },
            notification: function(args) {
              return 'Removed project';
            }
          },

          notification: {
            poll: testData.notifications.testPoll
          }
        },

        destroy: {
          label: 'Remove project',
          action: function(args) {
            args.response.success({});
          },

          messages: {
            confirm: function(args) {
              return 'Are you sure you want to remove ' + args.name + '?';
            },
            notification: function(args) {
              return 'Removed project';
            }
          },

          notification: {
            poll: testData.notifications.testPoll
          }
        }
      }
    }
  };
} (jQuery, cloudStack, testData));