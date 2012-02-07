(function(cloudStack) {
  cloudStack.projects = {
    requireInvitation: function(args) {
      return g_capabilities.projectinviterequired;
    },

    invitationCheck: function(args) {
      $.ajax({
        url: createURL('listProjectInvitations'),
        data: { state: 'Pending' },
        success: function(json) {
          args.response.success({
            data: json.listprojectinvitationsresponse.projectinvitation ?
              json.listprojectinvitationsresponse.projectinvitation : []
          });
        }
      });
    },

    resourceManagement: {
      update: function(args, projectID) {
        var totalResources = 5;
        var updatedResources = 0;

        projectID = projectID ? projectID : cloudStack.context.projects[0].id;

        $.each(args.data, function(key, value) {
          $.ajax({
            url: createURL('updateResourceLimit', { ignoreProject: true }),
            data: {
              projectid: projectID,
              resourcetype: key,
              max: args.data[key]
            },
            success: function(json) {
              updatedResources++;
              if (updatedResources == totalResources) {
                args.response.success();
              }
            }
          });
        });
      },

      dataProvider: function(args, projectID) {
        projectID = projectID ? projectID : cloudStack.context.projects[0].id;

        $.ajax({
          url: createURL('listResourceLimits', { ignoreProject: true }),
          data: {
            projectid: projectID
          },
          success: function(json) {
            args.response.success({
              data: $.map(
                json.listresourcelimitsresponse.resourcelimit,
                function(resource) {
                  var resourceMap = {
                    0: {
                      id: 'user_vm',
                      label: 'Max. User VMs'
                    },
                    1: {
                      id: 'public_ip',
                      label: 'Max. Public IPs'
                    },
                    2: {
                      id: 'volume',
                      label: 'Max. Volumes'
                    },
                    3: {
                      id: 'snapshot',
                      label: 'Max. Snapshots'
                    },
                    4: {
                      id: 'template',
                      label: 'Max. Templates'
                    },
                    5: {
                      id: 'project',
                      label: 'Max. Projects'
                    }
                  };
                  return {
                    id: resourceMap[resource.resourcetype].id,
                    label: resourceMap[resource.resourcetype].label,
                    type: resource.resourcetype,
                    value: resource.max
                  };
                }
              )
            });
          }
        });
      }
    },

    dashboard: function(args) {
      var dataFns = {
        instances: function(data) {
          $.ajax({
            url: createURL('listVirtualMachines'),
            success: function(json) {
              var instances = json.listvirtualmachinesresponse.virtualmachine ?
                    json.listvirtualmachinesresponse.virtualmachine : [];

              dataFns.storage($.extend(data, {
                runningInstances: $.grep(instances, function(instance) {
                  return instance.state == 'Running';
                }).length,
                stoppedInstances: $.grep(instances, function(instance) {
                  return instance.state != 'Running';
                }).length,
                totalInstances: instances.length
              }));
            }
          });
        },

        storage: function(data) {
          $.ajax({
            url: createURL('listVolumes'),
            success: function(json) {
              dataFns.bandwidth($.extend(data, {
                totalVolumes: json.listvolumesresponse.volume ?
                  json.listvolumesresponse.count : 0
              }));
            }
          });
        },

        bandwidth: function(data) {
          var totalBandwidth = 0;
          $.ajax({
            url: createURL('listNetworks'),
            success: function(json) {
              var networks = json.listnetworksresponse.network ?
                    json.listnetworksresponse.network : [];
              $(networks).each(function() {
                var network = this;
                $.ajax({
                  url: createURL('listNetworkOfferings'),
                  async: false,
                  data: {
                    id: network.networkofferingid
                  },
                  success: function(json) {
                    totalBandwidth +=
                    json.listnetworkofferingsresponse.networkoffering[0].networkrate;
                  }
                });
              });

              dataFns.ipAddresses($.extend(data, {
                totalBandwidth: totalBandwidth
              }));
            }
          });
        },

        ipAddresses: function(data) {
          $.ajax({
            url: createURL('listPublicIpAddresses'),
            success: function(json) {
              dataFns.loadBalancingRules($.extend(data, {
                totalIPAddresses: json.listpublicipaddressesresponse.count ?
                  json.listpublicipaddressesresponse.count : 0
              }));
            }
          });
        },

        loadBalancingRules: function(data) {
          $.ajax({
            url: createURL('listLoadBalancerRules'),
            success: function(json) {
              dataFns.portForwardingRules($.extend(data, {
                totalLoadBalancers: json.listloadbalancerrulesresponse.count ?
                  json.listloadbalancerrulesresponse.count : 0
              }));
            }
          });
        },

        portForwardingRules: function(data) {
          $.ajax({
            url: createURL('listPortForwardingRules'),
            success: function(json) {
              dataFns.users($.extend(data, {
                totalPortForwards: json.listportforwardingrulesresponse.count ?
                  json.listportforwardingrulesresponse.count : 0
              }));
            }
          });
        },

        users: function(data) {
          $.ajax({
            url: createURL('listProjectAccounts'),
            success: function(json) {
              var users = json.listprojectaccountsresponse.projectaccount;

              dataFns.events($.extend(data, {
                users: $.map(users, function(user) {
                  return {
                    account: user.account
                  };
                })
              }));
            }
          });
        },

        events: function(data) {
          $.ajax({
            url: createURL('listEvents', { ignoreProject: true }),
            data: {
              page: 1,
              pageSize: 8
            },
            success: function(json) {
              var events = json.listeventsresponse.event;

              complete($.extend(data, {
                events: $.map(events, function(event) {
                  return {
                    date: event.created.substr(5, 2) +
                      '/' + event.created.substr(8, 2) +
                      '/' + event.created.substr(2, 2),
                    desc: event.description
                  };
                })
              }));
            }
          });
        }
      };

      var complete = function(data) {
        args.response.success({
          data: data
        });
      };

      dataFns.instances();
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
      noHeaderActionsColumn: true,
      ignoreEmptyFields: true,
      fields: {
        'email': { edit: true, label: 'E-mail' },
        'account': { edit: true, label: 'Account' },
        'state': { edit: 'ignore', label: 'Status' },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Invite',
        action: function(args) {
          $.ajax({
            url: createURL('addAccountToProject', { ignoreProject: true }),
            data: {
              projectId: args.context.projects[0].id,
              account: args.data.account,
              email: args.data.email
            },
            dataType: 'json',
            async: true,
            success: function(data) {
              data: args.data,
              args.response.success({
                _custom: {
                  jobId: data.addaccounttoprojectresponse.jobid,
                  onComplete: function(json) {
                    if (args.data.email) {
                      cloudStack.dialog.notice({
                        message: 'Invitation sent to ' + args.data.email
                      });
                    }
                  }
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
        if (args.context.projects &&
            args.context.projects[0] &&
            !args.context.projects[0].isNew) {
          return args.context.actions;
        }

        return ['destroy'];
      },

      actions: {
        destroy: {
          label: 'Revoke invitation',
          action: function(args) {
            $.ajax({
              url: createURL('deleteProjectInvitation'),
              data: {
                id: args.context.multiRule[0].id
              },
              success: function(data) {
                args.response.success({
                  _custom: { jobId: data.deleteprojectinvitationresponse.jobid },
                  notification: {
                    label: 'Un-invited user',
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
          url: createURL('listProjectInvitations', { ignoreProject: true }),
          data: {
            listAll: true,
            projectId: args.context.projects[0].id
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            var invites = data.listprojectinvitationsresponse.projectinvitation ?
              data.listprojectinvitationsresponse.projectinvitation : [];
            args.response.success({
              data: $.map(invites, function(elem) {
                return {
                  id: elem.id,
                  account: elem.account,
                  email: elem.email,
                  state: elem.state
                };
              })
            });
          }
        });
      }
    },
    addUserForm: {
      noSelect: true,
      hideForm: function() {
        return g_capabilities.projectinviterequired;
      },
      fields: {
        'username': { edit: true, label: 'Account' },
        'role': { edit: 'ignore', label: 'Role' },
        'add-user': { addButton: true, label: '' }
      },
      add: {
        label: 'Add account',
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
              args.response.success({
                _custom: {
                  jobId: data.addaccounttoprojectresponse.jobid
                },
                notification: {
                  label: 'Added user to project',
                  poll: pollAsyncJobResult
                }
              });

              if (g_capabilities.projectinviterequired) {
                cloudStack.dialog.notice({ message: 'Invite sent to user; they will be added to the project once they accept the invitation' });
              }
            }
          });
        }
      },
      actionPreFilter: function(args) {
        if (!args.context.projects &&
            args.context.multiRule[0].role != 'Admin') { // This is for the new project wizard
            return ['destroy'];
        }

        if (args.context.multiRule[0].role != 'Admin') {
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
                id: args.context.projects[0].id,
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
                  role: elem.role,
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
          accountId: user.userid,
          listAll: true
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
    sectionSelect: {
      label: 'Select view'
    },
    sections: {
      projects: {
        type: 'select',
        id: 'projects',
        title: 'Projects',
        listView: {
          fields: {
            name: { label: 'Project Name' },
            displaytext: { label: 'Display Text' },
            domain: { label: 'Domain' },
            account: { label: 'Owner' },
            state: { label: 'Status', indicator: { 'Active': 'on', 'Destroyed': 'off', 'Disabled': 'off', 'Left Project': 'off' } }
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

            var apiCmd = "listProjects&page=" + args.page + "&pagesize=" + pageSize + array1.join("") + '&listAll=true';
            $.ajax({
              url: createURL(apiCmd, { ignoreProject: true }),
              dataType: 'json',
              async: true,
              success: function(data) {
                args.response.success({
                  data: data.listprojectsresponse.project,
                  actionFilter: projectsActionFilter
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
                            getUpdatedItem: function(data) {
                              return $.extend(data, { state: 'Destroyed' });
                            },
                            getActionFilter: function(args) {
                              return function() {
                                return [];
                              };
                            },
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
          },

          detailView: {
            actions: {
              edit: {
                label: 'Edit',
                action: function(args) {
                  $.ajax({
                    url: createURL('updateProject'),
                    data: $.extend(true, {}, args.context.projects[0], args.data),
                    success: function(json) {
                      args.response.success();
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },
                messages: {
                  notification: function(args) { return 'Edited project details'; }
                }
              },
              disable: {
                label: 'Suspend project',
                action: function(args) {
                  $.ajax({
                    url: createURL('suspendProject'),
                    data: {
                      id: args.context.projects[0].id
                    },
                    success: function(json) {
                      args.response.success({
                        _custom: {
                          jobId: json.suspendprojectresponse.jobid,
                          getUpdatedItem: function() {
                            return { state: 'Suspended' };
                          }
                        }
                      });
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },
                messages: {
                  confirm: function() { return 'Are you sure you want to suspend this project?'; },
                  notification: function() { return 'Suspended project'; }
                },
                notification: { poll: pollAsyncJobResult }
              },

              enable: {
                label: 'Activate project',
                action: function(args) {
                  $.ajax({
                    url: createURL('activateProject'),
                    data: {
                      id: args.context.projects[0].id
                    },
                    success: function(json) {
                      args.response.success({
                        _custom: {
                          jobId: json.activaterojectresponse.jobid, // NOTE: typo
                          getUpdatedItem: function() {
                            return { state: 'Active' };
                          }
                        }
                      });
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                },
                messages: {
                  confirm: function() { return 'Are you sure you want to activate this project?'; },
                  notification: function() { return 'Activated project'; }
                },
                notification: { poll: pollAsyncJobResult }
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
                          getUpdatedItem: function(data) {
                            return $.extend(data, { state: 'Destroyed' });
                          },
                          getActionFilter: function(args) {
                            return function() {
                              return [];
                            };
                          },
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
            },
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    displaytext: { label: 'Display text', isEditable: true },
                    domain: { label: 'Domain' },
                    account: { label: 'Account'},
                    state: { label: 'State' }
                  }
                ],
                dataProvider: function(args) {
                  var projectID = args.context.projects[0].id;

                  $.ajax({
                    url: createURL('listProjects'),
                    data: {
                      listAll: true,
                      id: projectID
                    },
                    success: function(json) {
                      args.response.success({
                        data: json.listprojectsresponse.project[0],
                        actionFilter: projectsActionFilter
                      });
                    }
                  });
                }
              },

              accounts: {
                title: 'Accounts',
                custom: function(args) {
                  var project = args.context.projects[0];
                  var multiEditArgs = $.extend(
                    true, {},
                    cloudStack.projects.addUserForm,
                    {
                      context: { projects: [project] }
                    }
                  );
                  var $users = $('<div>').multiEdit(multiEditArgs);
                  
                  return $users;
                }
              },

              invitations: {
                title: 'Invitations',
                custom: function(args) {
                  var project = args.context.projects[0];
                  var $invites = cloudStack.uiCustom.projectsTabs.userManagement({
                    useInvites: true,
                    context: { projects: [project] }
                  });
                  
                  return $invites;
                }
              },

              resources: {
                title: 'Resources',
                custom: function(args) {
                  var $resources = cloudStack.uiCustom
                    .projectsTabs.dashboardTabs.resources({
                      projectID: args.context.projects[0].id
                  });

                  return $('<div>').addClass('project-dashboard').append($resources);
                }
              }
            }
          }
        }
      },

      invitations: {
        type: 'select',
        id: 'invitations',
        title: 'Invitations',
        listView: {
          fields: {
            project: { label: 'Project' },
            domain: { label: 'Domain' },
            state: {
              label: 'Status',
              indicator: {
                'Accepted': 'on', 'Completed': 'on',
                'Pending': 'off', 'Declined': 'off'
              }
            }
          },

          dataProvider: function(args) {
            $.ajax({
              url: createURL('listProjectInvitations'),
              data: {
                listAll: true,
                account: cloudStack.context.users[0].account,
                domainid: cloudStack.context.users[0].domainid,
                state: 'Pending'
              },
              success: function(data) {
                args.response.success({
                  actionFilter: projectInvitationActionFilter,
                  data: data.listprojectinvitationsresponse.projectinvitation ?
                    data.listprojectinvitationsresponse.projectinvitation : []
                });
              }
            });
          },

          actions: {
            enterToken: {
              label: 'Enter Token',
              isHeader: true,
              addRow: false,
              createForm: {
                desc: 'Please enter the token that you were given in your invite e-mail.',
                fields: {
                  projectid: { label: 'Project ID' },
                  token: { label: 'Token' }
                }
              },
              action: function(args) {
                $.ajax({
                  url: createURL('updateProjectInvitation'),
                  data: args.data,
                  success: function(json) {
                    args.response.success({
                      _custom: {
                        jobId: json.updateprojectinvitationresponse.jobid
                      }
                    });
                  },
                  error: function(json) {
                    args.response.error(parseXMLHttpResponse(json));
                  }
                });
              },
              messages: {
                notification: function() {
                  return 'Accepted project invitation';
                },
                complete: function() {
                  return 'You have now joined a project. Please switch to Project view to see the project.';
                }
              },
              notification: { poll: pollAsyncJobResult }
            },
            
            accept: {
              label: 'Accept Invitation',
              action: function(args) {
                $.ajax({
                  url: createURL('updateProjectInvitation'),
                  data: {
                    projectid: args.context.invitations[0].projectid,
                    account: args.context.users[0].account,
                    domainid: args.context.users[0].domainid,
                    accept: true
                  },
                  success: function(data) {
                    args.response.success({
                      _custom: {
                        jobId: data.updateprojectinvitationresponse.jobid,
                        getUpdatedItem: function() { return { state: 'Accepted' }; }
                      }
                    });
                  }
                });
              },
              messages: {
                confirm: function() { return 'Please confirm you wish to join this project.'; },
                notification: function() { return 'Accepted project invitation'; }
              },
              notification: { poll: pollAsyncJobResult }
            },

            decline: {
              label: 'Decline Invitation',
              action: function(args) {
                $.ajax({
                  url: createURL('updateProjectInvitation'),
                  data: {
                    projectid: args.context.invitations[0].projectid,
                    account: args.context.users[0].account,
                    accept: false
                  },

                  success: function(data) {
                    args.response.success({
                      _custom: {
                        jobId: data.updateprojectinvitationresponse.jobid,
                        getUpdatedItem: function() { return { state: 'Declined' }; }
                      }
                    });
                  }
                });
              },
              notification: { poll: pollAsyncJobResult },
              messages: {
                confirm: function() { return 'Are you sure you want to decline this project invitation?'; },
                notification: function() { return 'Declined project invitation'; }
              }
            }
          }
        }
      }
    }
  };

  var projectsActionFilter = function(args) {
    var allowedActions = ['destroy', 'edit'];

    if (args.context.item.account == cloudStack.context.users[0].account ||
        isAdmin() || isDomainAdmin()) {
      if (args.context.item.state == 'Suspended') {
        allowedActions.push('enable');
      } else if (args.context.item.state == 'Active') {
        allowedActions.push('disable');
      }

      return allowedActions;
    }

    return [];
  };

  var projectInvitationActionFilter = function(args) {
    var state = args.context.item.state;

    if (state == 'Accepted' || state == 'Completed' || state == 'Declined') {
      return [];
    }

    return ['accept', 'decline'];
  };
} (cloudStack));
