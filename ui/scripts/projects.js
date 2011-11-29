(function(cloudStack, testData) {
  cloudStack.projects = {
    requireInvitation: function(args) {
      return window.g_projectsInviteRequired;
    },
    
    add: function(args) {
      setTimeout(function() {
        $.ajax({
          url: createURL('createProject', { ignoreProject: true }),
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
          },
          error: function() {
            args.response.error('Could not create project.');
          }
        });
      }, 100);
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
          $.ajax({
            url: createURL('addAccountToProject', { ignoreProject: true }),
            data: {
              projectId: args.context.projects[0].id,
              email: args.data.email
            },
            dataType: 'json',
            async: true,
            success: function(data) {
              data: args.data,
              args.response.success({
                _custom: {
                  jobId: data.addaccounttoprojectresponse.jobid
                },
                notification: {
                  label: 'Invited user to project',
                  poll: pollAsyncJobResult
                }
              });
            },
            error: function(data) {
              args.response.error('Could not create user');
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

      actions: {},

      // Project users data provider
      dataProvider: function(args) {
        $.ajax({
          url: createURL('listProjectInvitations', { ignoreProject: true }),
          data: {
            projectId: args.context.projects[0].id
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            var invites = data.listprojectinvitationsresponse.projectinvitation;
            args.response.success({
              data: $.map(invites, function(elem) {
                return {
                  id: elem.id,
                  email: elem.email ? elem.email : elem.account
                };
              })
            });
          }
        });
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
          $.ajax({
            url: createURL('addAccountToProject', { ignoreProject: true }),
            data: {
              projectId: args.context.projects[0].id,
              account: args.data.username
            },
            dataType: 'json',
            async: true,
            success: function(data) {
              data: args.data,
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
        if (!cloudStack.context.projects) { // This is for the new project wizard
          return ['destroy'];
        }

        var project = cloudStack.context.projects[0];
        var projectOwner = project.account;
        var rowAccount = args.context.multiRule[0].account;
        var userAccount = cloudStack.context.users[0].account;
        var isEditableRow = rowAccount != projectOwner && userAccount == projectOwner;

        if (isEditableRow) {
          return args.context.actions;
        }

        return [];
      },
      actions: {
        destroy: {
          label: 'Remove user from project',
          action: function(args) {
            $.ajax({
              url: createURL('deleteAccountFromProject', { ignoreProject: true }),
              data: {
                projectId: args.context.projects[0].id,
                account: args.context.multiRule[0].username
              },
              dataType: 'json',
              async: true,
              success: function(data) {
                args.response.success({
                  _custom: {
                    jobId: data.deleteaccountfromprojectresponse.jobid
                  },
                  notification: {
                    label: 'Removed user from project',
                    poll: pollAsyncJobResult
                  }
                });
              },
              error: function(data) {
                args.response.error('Could not remove user');
              }
            });
          }
        },

        makeOwner: {
          label: 'Make user project owner',
          action: function(args) {
            $.ajax({
              url: createURL('updateProject', { ignoreProject: true }),
              data: {
                id: cloudStack.context.projects[0].id,
                account: args.context.multiRule[0].username
              },
              dataType: 'json',
              async: true,
              success: function(data) {
                args.response.success({
                  _custom: {
                    jobId: data.updateprojectresponse.jobid
                  },
                  notification: {
                    label: 'Assigned new project owner',
                    poll: pollAsyncJobResult
                  }
                });
              }
            });
          }
        }
      },

      // Project users data provider
      dataProvider: function(args) {
        $.ajax({
          url: createURL('listProjectAccounts', { ignoreProject: true }),
          data: {
            projectId: args.context.projects[0].id
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            args.response.success({
              data: $.map(data.listprojectaccountsresponse.projectaccount, function(elem) {
                return {
                  id: elem.accountid,
                  username: elem.role == 'Owner' ?
                    elem.account + ' (owner)' : elem.account
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
        url: createURL('listProjects', { ignoreProject: true }),
        data: {
          accountId: user.userid
        },
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            data: $.map(
              data.listprojectsresponse.project ?
                data.listprojectsresponse.project : [],
              function(elem) {
              return $.extend(elem, {
                displayText: elem.displaytext
              });
            })
          });
        }
      });
    }
  };

  cloudStack.sections.projects = {
    title: 'Projects',
    id: 'projects',
    listView: {
      fields: {
        name: { label: 'Project Name' },
        displaytext: { label: 'Display Text' },
        domain: { label: 'Domain' },
        account: { label: 'Owner' }
      },

      dataProvider: function(args) {
        $.ajax({
          url: createURL('listProjects', { ignoreProject: true }),
          dataType: 'json',
          async: true,
          success: function(data) {
            args.response.success({
              data: data.listprojectsresponse.project
            });
          }
        });
      },

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
            $.ajax({
              url: createURL('deleteProject', { ignoreProject: true }),
              data: {
                id: args.data.id
              },
              dataType: 'json',
              async: true,
              success: function(data) {
                args.response.success({
                  _custom: {
                    jobId: data.deleteprojectresponse.jobid
                  }
                });
              }
            });
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
            poll: pollAsyncJobResult
          }
        }
      }
    }
  };
} (cloudStack, testData));
