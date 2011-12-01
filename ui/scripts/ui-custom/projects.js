(function(cloudStack, $) {
  var pageElems = {
    /**
     * User management multi-edit
     */
    userManagement: function(args) {
      var multiEdit = !args.useInvites ? 
            cloudStack.projects.addUserForm :
            cloudStack.projects.inviteForm;

      return $('<div>').multiEdit($.extend(true, {}, multiEdit, {
        context: args.context
      }));
    },

    /**
     * Projects dashboard
     */
    dashboard: function() {
      var tabs = {
        Overview: function() {
          return $('<img>').attr('src', 'images/screens/ProjectDashboard.png');
        }
      };

      if (cloudStack.context.projects) {
        tabs['Users'] = function() {
          return $('<div>').addClass('management');
        };

        tabs['Invitations'] = function() {
          return $('<div>').addClass('management-invite');
        };
      }

      var $tabs = $('<div>').addClass('tab-content').append($('<ul>'));
      var $toolbar = $('<div>').addClass('toolbar');

      // Make UI tabs
      $.each(tabs, function(tabName, tab) {
        var $tab = $('<li>').appendTo($tabs.find('ul'));
        var $tabLink = $('<a>')
              .attr({ href: '#project-view-dashboard-' + tabName })
              .html(tabName)
              .appendTo($tab);
        var $content = $('<div>')
              .appendTo($tabs)
              .attr({ id: 'project-view-dashboard-' + tabName })
              .append(tab());
      });

      $tabs.find('ul li:first').addClass('first');
      $tabs.find('ul li:last').addClass('last');

      $tabs.bind('tabsshow', function(event, ui) {
        var $panel = $(ui.panel);
        var $management = $panel.find('.management');
        var $managementInvite = $panel.find('.management-invite');

        if ($management.size()) {
          $management.children().remove();
          $management.append(pageElems.userManagement({
            context: cloudStack.context
          }));

          return true;
        }

        if ($managementInvite.size()) {
          $managementInvite.children().remove();
          $managementInvite.append(pageElems.userManagement({
            context: cloudStack.context,
            useInvites: true
          }));
        }

        return true;
      });

      return $('<div>').addClass('project-dashboard')
        .append($.merge(
          $toolbar,
          $tabs.tabs()
        ));
    },

    /**
     * Add new project flow
     */
    newProjectForm: function() {
      var $newProject = $('<div>').addClass('new-project');
      $newProject.append($('<div>').addClass('title').html('Create a project'));

      var $form = $('<form>');
      var $formDesc = $('<div>').addClass('form-desc');
      var $projectName = $('<div>').addClass('field name')
            .append($('<label>').attr('for', 'project-name').html('Project name'))
            .append($('<input>').addClass('required').attr({
              type: 'text',
              name: 'project-name'
            }));
      var $projectDesc = $('<div>').addClass('field desc')
            .append($('<label>').attr('for', 'project-desc').html('Display text'))
            .append($('<input>').attr({
              type: 'text',
              name: 'project-display-text'
            }));
      var $submit = $('<input>').attr({ type: 'submit' }).val('Create Project');
      var $cancel = $('<div>').addClass('button cancel').html('Cancel');
      var $loading = $('<div>').addClass('loading-overlay');

      // Form events/validation
      $form.validate();
      $form.submit(function() {
        if (!$form.valid()) return false;

        $form.prepend($loading);

        cloudStack.projects.add({
          context: cloudStack.context,
          data: cloudStack.serializeForm($form),
          response: {
            success: function(args) {
              var project = args.data;

              $(window).trigger('cloudStack.fullRefresh');
              
              $loading.remove();

              // Confirmation
              $form.replaceWith(function() {
                var $confirm = $('<div>').addClass('confirm');

                // Update title with project name
                $newProject.find('.title').html(args.data.name);

                // Show field data
                $confirm.append($projectName).find('input').replaceWith( // Name
                  $('<span>').addClass('value').html(
                    args.data.name
                  )
                );
                $confirm.append($projectDesc).find('input').replaceWith( // Display text
                  $('<span>').addClass('value').html(
                    args.data.displayText
                  )
                );

                var $buttons = $('<div>').addClass('buttons');
                var $addAccountButton = $('<div>').addClass('button confirm').html('Add Accounts');

                $addAccountButton.click(function() {
                  // Show add user form
                  $confirm.replaceWith(function() {
                    var $userManagement = pageElems.userManagement({
                      context: $.extend(true, {}, cloudStack.context, {
                        projects: [project]
                      }),
                      useInvites: cloudStack.projects.requireInvitation()
                    });
                    var $nextButton = $('<div>').addClass('button confirm next').html('Next');

                    $newProject.find('.title').html('Add Accounts to ' + args.data.name);
                    $nextButton.appendTo($userManagement).click(function() {
                      $newProject.find('.title').html('Review');
                      $userManagement.replaceWith(function() {
                        var $review = $('<div>').addClass('review');
                        var $projectData = $('<div>').addClass('project-data');

                        // Basic project data
                        $review.append($projectData);
                        $projectData.append($projectName).find('input').replaceWith( // Name
                          $('<span>').addClass('value').html(
                            args.data.name
                          )
                        );
                        $projectData.append($projectDesc).find('input').replaceWith( // Display text
                          $('<span>').addClass('value').html(
                            args.data.displayText
                          )
                        );

                        // User/resouce tabs
                        var $tabs = $('<div>').addClass('tabs resources').appendTo($review);
                        var $ul = $('<ul>').appendTo($tabs)
                              .append(
                                // Users tab
                                $('<li>').addClass('first').append(
                                  $('<a>').attr({ href: '#new-project-review-tabs-users'}).html('Users')
                                )
                              )
                              .append(
                                // Resources tab
                                $('<li>').addClass('last').append(
                                  $('<a>').attr({ href: '#new-project-review-tabs-resouces'}).html('Resources')
                                )
                              );

                        var $users = $('<div>').attr({ id: 'new-project-review-tabs-users' }).appendTo($tabs);
                        var $resouces = $('<div>').attr({ id: 'new-project-review-tabs-resouces' }).appendTo($tabs).html('(Resources go here)');

                        $tabs.tabs();

                        $users.listView({
                          listView: {
                            id: 'project-accounts',
                            disableInfiniteScrolling: true,
                            fields: !cloudStack.projects.requireInvitation() ? {
                              username: { label: 'Account' }
                            } : {
                              email: { label: 'E-mail invite'}
                            },
                            actions: !cloudStack.projects.requireInvitation() ? {
                              destroy: {
                                label: 'Remove account from project',
                                action: {
                                  custom: function(args) {
                                    var $instanceRow = args.$instanceRow;

                                    $instanceRow.animate({ opacity: 0.5 });
                                    
                                    cloudStack.projects.addUserForm.actions.destroy.action({
                                      context: $.extend(true, {}, cloudStack.context, {
                                        projects: [project],
                                        multiRule: [{
                                          username: $instanceRow.find('td.username span').html()
                                        }]
                                      }),
                                      response: {
                                        success: function(args) {
                                          $instanceRow.remove();
                                        }
                                      }
                                    });
                                  }
                                }
                              }
                            } : {},
                            dataProvider: function(args) {
                              setTimeout(function() {
                                args.response.success({
                                  data: $.map($userManagement.find('.data-item tr'), function(elem) {
                                    // Store previous user data in list table
                                    return !cloudStack.projects.requireInvitation() ? {
                                      username: $(elem).find('td.username span').html()
                                    } : {
                                      email: $(elem).find('td.email span').html()
                                    };
                                  })
                                });
                              }, 0);
                            }
                          }
                        });

                        // Save button
                        var $saveButton = $nextButton.clone().appendTo($review);
                        $saveButton.html('Save');
                        $saveButton.click(function() {
                          $('.ui-dialog, .overlay').remove();
                        });

                        return $review;
                      });
                    });

                    return $userManagement;
                  });

                  return false;
                });

                var $laterButton = $('<div>').addClass('button later').html('Remind me later');
                $laterButton.click(function() {
                  $(':ui-dialog, .overlay').remove();

                  return false;
                });

                $buttons.appendTo($confirm).append($.merge(
                  $addAccountButton, $laterButton
                ));

                return $confirm;
              });
            },
            error: cloudStack.dialog.error(function() {
              $loading.remove();
            })
          }
        });

        return false;
      });

      $cancel.click(function() {
        $(':ui-dialog, .overlay').remove();
      });

      return $newProject
        .append(
          $form
            .append($formDesc)
            .append($projectName)
            .append($projectDesc)
            .append($cancel)
            .append($submit)
        );
    },

    /**
     * Project selection list
     */
    selector: function(args) {
      var $selector = $('<div>').addClass('project-selector');
      var $toolbar = $('<div>').addClass('toolbar');
      var $list = $('<div>').addClass('listing')
            .append($('<div>').addClass('header').html('Name'))
            .append($('<div>').addClass('data').append($('<ul>')));
      var $searchForm = $('<form>');
      var $search = $('<div>').appendTo($toolbar).addClass('search')
            .append(
              $searchForm
                .append($('<input>').attr({ type: 'text' }))
                .append($('<input>').attr({ type: 'submit' }).val(''))
            );
      var $projectSelect = args.$projectSelect;

      // Get project data
      var loadData = function(complete) {
        cloudStack.projects.dataProvider({
          context: cloudStack.context,
          response: {
            success: function(args) {
              var data = args.data;

              $projectSelect.find('option').remove();
              $(data).each(function() {
                var displayText = this.displayText ? this.displayText : this.name;

                $('<li>')
                  .data('json-obj', this)
                  .html(displayText)
                  .appendTo($list.find('ul'));

                // Populate project select
                $('<option>')
                  .appendTo($projectSelect)
                  .data('json-obj', this)
                  .html(displayText);
              });

              cloudStack.evenOdd($list, 'li', {
                even: function($elem) {
                  $elem.addClass('even');
                },

                odd: function($elem) {
                  $elem.addClass('odd');
                }
              });

              if (complete) complete();
            }
          }
        });
      };

      // Search form
      $searchForm.submit(function() {
        $list.find('li').remove();
        loadData();

        return false;
      });

      // Initial load
      loadData(function() {
        if (!$list.find('li').size()) {
          cloudStack.dialog.notice({
            message: 'You do not have any projects. '
              + 'Please create a new project from the projects section.'
          }).closest('.ui-dialog');
          $.merge($selector, $('.overlay')).remove();
          $('.select.default-view').click();
        } else {
          $selector.dialog({
            title: 'Select Project',
            dialogClass: 'project-selector-dialog',
            width: 420
          }).closest('.ui-dialog').overlay();
          showDashboard();
        }
      });

      // Project item click event
      $selector.click(function(event) {
        var $target = $(event.target);

        if ($target.is('li')) {
          cloudStack.context.projects = [$target.data('json-obj')];
          showDashboard();
          $.merge($selector, $('.overlay')).remove();

          // Select active project
          $projectSelect
            .find('option').attr('selected', '')
            .filter(function() {
              return $(this).data('json-obj').name == cloudStack.context.projects[0].name;
            }).attr('selected', 'selected');

          ////
          // Hidden for now
          //$projectSelect.parent().show();
        }
      });

      return $selector
        .append($toolbar)
        .append($list);
    }
  };

  /**
   * Show project-mode appearance on CloudStack UI
   */
  var applyProjectStyle = function() {
    var $container = $('#cloudStack3-container');
    $container.addClass('project-view');
  };

  /**
   * Initiate new project flow
   */
  var addProject = function() {
    pageElems.newProjectForm().dialog({
      title: 'New Project',
      width: 760
    }).closest('.ui-dialog').overlay();
  };

  /**
   * Show the dashboard, in panel
   */
  var showDashboard = function() {
    var $browser = $('#browser .container');
    applyProjectStyle($('html body'));

    // Cleanup project context
    if (cloudStack.context.projects)
      cloudStack.context.projects[0].isNew = false;

    $browser.cloudBrowser('removeAllPanels');
    $browser.cloudBrowser('addPanel', {
      title: 'Project Dashboard',
      complete: function($newPanel) {
        $('#navigation li.dashboard').addClass('active').siblings().removeClass('active');
        $newPanel.append(pageElems.dashboard);
      }
    });
  };

  /**
   * Projects entry point
   */
  cloudStack.uiCustom.projects = function(args) {
    var $dashboardNavItem = $('#navigation li.navigation-item.dashboard');
    var $projectSelect = args.$projectSelect;

    // Use project dashboard
    var event = function() {
      if (!$('#cloudStack3-container').hasClass('project-view')) {
        // No longer in project view, go back to normal dashboard
        $dashboardNavItem.unbind('click', event);

        return true;
      }

      $(this).addClass('active');
      $(this).siblings().removeClass('active');

      if (cloudStack.context.projects && cloudStack.context.projects[0])
        showDashboard();

      return false;
    };
    $dashboardNavItem.bind('click', event);

    // Project selector event
    $projectSelect.change(function() {
      cloudStack.context.projects = [
        $projectSelect.find('option:selected').data('json-obj')
      ];

      $(window).trigger('cloudStack.fullRefresh');
    });

    pageElems.selector(args);
  };

  /**
   * New project event
   */
  $(window).bind('cloudStack.newProject', function() {
    addProject();
  });
})(cloudStack, jQuery);
