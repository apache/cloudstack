(function(cloudStack, testData) {
  cloudStack.projects = {
    add: function(args) {
      setTimeout(function() {
        $.ajax({
          url: createURL('createProject'),
          data: {
            account: args.context.users[0].account,
            domainId: args.context.users[0].domainid,
            name: args.data['project-name'],
            displayText: args.data['project-display-text']
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            args.response.success({
              data: {
                id: data.createprojectresponse.id,
                name: args.data['project-name'],
                displayText: args.data['project-display-text'],
                users: []
              }
            });
          }
        });
      }, 100);
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
                { name: 'User', description: 'User' },
                { name: 'Admin', description: 'Admin' }
              ]
            });
          }
        },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Invite',
        action: function(args) {
          $.ajax({
            url: createURL('addAccountToProject'),
            data: {
              projectId: cloudStack.context.projects[0].id,
              account: args.data.username
            },
            dataType: 'json',
            async: true,
            success: function(data) {
              args.response.success({
                _custom: {
                  jobId: data.addaccounttoprojectresponse.jobid
                },
                notification: {
                  label: 'Added user to project',
                  poll: pollAsyncJobResult
                } 
              });
            }
          });
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
        $.ajax({
          url: createURL('listProjectAccounts'),
          data: {
            role: 'Admin, User',
            projectId: cloudStack.context.projects[0].id
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            args.response.success({
              data: $.map(data.listprojectaccountsresponse.projectaccount, function(elem) {
                return {
                  id: elem.accountid,
                  username: elem.account,
                  role: elem.role
                };
              })
            });
          }
        });
      }
    },

    // Project listing data provider
    dataProvider: function(args) {
      var user = args.context.users[0];

      $.ajax({
        url: createURL('listProjects'),
        data: {
          accountId: user.userid
        },
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            data: $.map(data.listprojectsresponse.project, function(elem) {
              return $.extend(elem, {
                displayText: elem.displaytext
              });
            })
          });
        }
      });
    }
  };
} (cloudStack, testData));