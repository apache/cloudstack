(function(cloudStack, $) {
  var pageElems = {
    /**
     * User management multi-edit
     */
    userManagement: function() {
      var multiEdit = cloudStack.projects.addUserForm;

      return $('<div>').multiEdit(multiEdit);
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
        tabs['Manage'] = function() {
          return $('<div>').addClass('management');
        };
      }
      
      var $tabs = $('<div>').addClass('tab-content').append($('<ul>'));
      var $toolbar = $('<div>').addClass('toolbar')
            .append(
              $('<div>').addClass('button add')
                .append($('<span>').html('New Project'))
                .click(function() {
                  addProject();
                })
            );

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

        if ($management.size()) {
          $management.children().remove();
          $management.append(pageElems.userManagement());
        }
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
            .append($('<label>').attr({ for: 'project-name' }).html('Project name'))
            .append($('<input>').addClass('required').attr({
              type: 'text',
              name: 'project-name'
            }));
      var $projectDesc = $('<div>').addClass('field desc')
            .append($('<label>').attr({ for: 'project-desc' }).html('Display text'))
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
          data: cloudStack.serializeForm($form),
          response: {
            success: function(args) {
              // Add project data to context
              cloudStack.context.projects = [args.data];
              cloudStack.context.projects[0].isNew = true;
              
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
                    var $userManagement = pageElems.userManagement();
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

                            fields: {
                              username: { label: 'Account' },
                              role: { label: 'Role' }
                            },
                            actions: {
                              editRole: {
                                label: 'Edit account role',
                                action: {
                                  custom: function(args) {
                                    var $instanceRow = args.$instanceRow;
                                    var $role = $instanceRow.find('td.role');
                                    var role = $role.find('span').html();
                                    var $select = $('<select>');
                                    var $editButtons = $.merge(
                                      $('<div>').addClass('action save'),
                                      $('<div>').addClass('action cancel')
                                    );

                                    // Edit actions
                                    $editButtons.click(function(event) {
                                      var $target = $(event.target);

                                      return false;
                                    });

                                    $role.addClass('editable');
                                    $select.append(
                                      $userManagement.find('option').clone()
                                    );

                                    $role.html('')
                                      .append(
                                        $('<div>').addClass('edit')
                                          .append($select)
                                          .append($editButtons)

                                      );

                                    $instanceRow.closest('.data-table').dataTable('refresh');
                                  }
                                }
                              },
                              destroy: {
                                label: 'Remove account from project',
                                action: {
                                  custom: function(args) {

                                  }
                                }
                              }
                            },
                            dataProvider: function(args) {
                              setTimeout(function() {
                                args.response.success({
                                  data: cloudStack.context.projects[0].users
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
                          showDashboard();
                        });

                        return $review;
                      });
                    });

                    return $userManagement;
                  });
                });

                var $laterButton = $('<div>').addClass('button later').html('Remind me later');
                $laterButton.click(function() {
                  $(':ui-dialog, .overlay').remove();
                  showDashboard();
                });

                $buttons.appendTo($confirm).append($.merge(
                  $addAccountButton, $laterButton
                ));

                return $confirm;
              });
            }
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
    selector: function(dataProvider) {
      // Get project data
      var loadData = function(complete) {
        cloudStack.projects.dataProvider({
          context: cloudStack.context,
          response: {
            success: function(args) {
              var data = args.data;

              $(data).each(function() {
                $('<li>')
                  .data('json-obj', this)
                  .html(this.displayText)
                  .appendTo($list.find('ul'));
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

      // Search form
      $searchForm.submit(function() {
        $list.find('li').remove();
        loadData();

        return false;
      });

      // Initial load
      loadData(function() {
        if (!$list.find('li').size()) {
          showDashboard();
          $.merge($selector, $('.overlay')).remove();

          return;
        } else {
          $selector.dialog({
            title: 'Select Project',
            dialogClass: 'project-selector-dialog',
            width: 420
          }).closest('.ui-dialog').overlay();
        }
      });

      // Project item click event
      $selector.click(function(event) {
        var $target = $(event.target);

        if ($target.is('li')) {
          cloudStack.context.projects = [$target.data('json-obj')];
          showDashboard();
          $.merge($selector, $('.overlay')).remove();
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
  cloudStack.uiCustom.projects = function() {
    var $dashboardNavItem = $('#navigation li.navigation-item.dashboard');

    // Use project dashboard
    var event = function() {
      if (!$('#cloudStack3-container').hasClass('project-view')) {
        // No longer in project view, go back to normal dashboard
        $dashboardNavItem.unbind('click', event);

        return true;
      }

      showDashboard();
      $(this).addClass('active');
      $(this).siblings().removeClass('active');

      return false;
    };
    $dashboardNavItem.bind('click', event);

    pageElems.selector();
  };
})(cloudStack, jQuery);