(function($, cloudStack) {
  /**
   * Make system zone 'org' chart
   */
  cloudStack.zoneChart = function(args) {
    return function(listViewArgs) {
      var naas = cloudStack.sections.system.naas;
      var $browser = listViewArgs.$browser;
      var $charts = $('<div>').addClass('system-charts');
      var context = listViewArgs.context;

      /**
       * Generates provider-wide actions
       */
      var providerActions = function(actions, options) {
        if (!options) options = {};

        var $actions = $('<div>').addClass('main-actions');
        var allowedActions = options.actionFilter ? options.actionFilter({
          actions: $.map(actions, function(value, key) { return key; })
        }) : null;

        $.each(actions, function(actionID, action) {
          if (allowedActions && $.inArray(actionID, allowedActions) == -1)
            return true;
          
          var $action = $('<div>').addClass('button action main-action');

          $action.addClass(actionID);
          $action.append($('<span>').addClass('icon'));
          $action.append($('<span>').html(action.label));
          $action.click(function() {
            action.action({
              context: { zones: listViewArgs.context.physicalResources },
              response: {
                success: function(args) {
                  if (options.success) options.success($.extend(args, {
                    action: action
                  }));
                }
              }
            });
          });

          $action.appendTo($actions);

          return true;
        });

        return $actions;
      };

      /**
       * Render specified network's system chart
       */
      var chartView = function(network) {
        var $chartView = $('<div>').addClass('system-chart-view')
              .append($('#template').find('div.zone-chart').clone());
        var $naasView = $chartView.find('.resources.naas ul.system-main');
        var networkStatus = naas.networkProviders.statusCheck({
          context: $.extend(true, {}, context, {
            systemNetworks: [network]
          })
        });

        // Update title
        var $title = $chartView.find('.head span');
        $title.html($title.html() + ' - ' + network.name);

        // Render network provider items
        var $networkProviders = $('<ul>')
              .appendTo(
                $('<li>').addClass('network-providers').appendTo($naasView)
              );

        $.each(naas.networkProviders.types, function(name, type) {
          var status = networkStatus[name];
          var statusLabel = naas.networkProviders.statusLabels ? 
                naas.networkProviders.statusLabels[status] : {};

          var $item = $('<li>').addClass('provider')
                .attr('rel', name)
                .attr('network-status', status)
                .addClass(name).addClass(status)
                .appendTo($networkProviders)
                .append($('<div>').addClass('name').html(type.label))
                .append($('<div>').addClass('status')
                        .append($('<span>').html(
                          statusLabel ? statusLabel : status
                        )))
                .append($('<div>').addClass('view-all configure').html('Configure'));
        });

        // View all action
        $chartView.find('ul li div.view-all').click(function() {
          var $target = $(this);

          if ($target.hasClass('configure')) return false;

          var $panel = $browser.cloudBrowser('addPanel', {
            title: $target.closest('li').find('div.name span').html(),
            data: '',
            noSelectPanel: true,
            maximizeIfSelected: true,
            complete: function($newPanel) {
              $panel.listView(
                $.extend(cloudStack.sections.system.subsections[
                  $target.attr('zone-target')
                ], {
                  $browser: $browser,
                  $chartView: $chartView,
                  ref: { zoneID: listViewArgs.id },
                  context: { zones: listViewArgs.context.physicalResources }
                })
              );
            }
          });

          return false;
        });

        // View details action
        $chartView.find('ul li div.view-details').click(function() {
          var $target = $(this);
          var $panel = $browser.cloudBrowser('addPanel', {
            title: 'Zone Details',
            data: '',
            noSelectPanel: true,
            maximizeIfSelected: true,
            complete: function($newPanel) {
              // Create detail view
              $.extend(args.detailView, {
                id: listViewArgs.id,
                context: { zones: listViewArgs.context.physicalResources },
                $browser: listViewArgs.$browser
              });

              $panel.detailView(args.detailView);
            }
          });

          return false;
        });

        // Add Resource button action
        $chartView.find('#add_resource_button').click(function() {
          var completeAction = function() { return false; };
          var $addResource = $('<div>').addClass('add-zone-resource');
          var $header = $('<div>').addClass('head').appendTo($addResource)
                .append(
                  $('<span>').addClass('message').html('Select resource to add:')
                );
          var $select = $('<select>').change(function() {
            var action = cloudStack.sections.system.subsections[$select.val()]
                  .listView.actions.add;
            var createForm = action.createForm;

            $addResource.find('.form-container').remove();

            // Create dialog
            var formData = cloudStack.dialog.createForm({
              form: createForm,
              context: { zones: listViewArgs.context.physicalResources },
              after: function(args) {
                action.action($.extend(args, {
                  response: {
                    success: function(args) {
                      $('div.notifications').notifications('add', {
                        desc: action.messages.notification({}),
                        interval: 1000,
                        poll: action.notification.poll,
                        _custom: args ? args._custom : null
                      });
                    }
                  }
                }));
              },
              noDialog: true
            });

            var $formContainer = formData.$formContainer
                  .appendTo($addResource).validate();
            completeAction = formData.completeAction;

            $(':ui-dialog').dialog('option', 'position', 'center');
          });

          // Append list of 'add new' items, based on subsection actions
          $.each(cloudStack.sections.system.subsections, function(sectionID, section) {
            var addAction = section.listView && section.listView.actions ?
                  section.listView.actions.add : null;

            if (addAction) {
              $('<option>').appendTo($select)
                .html(section.title)
                .val(sectionID);
            }
          });

          $header.append($select);
          $addResource.dialog({
            dialogClass: 'create-form',
            width: 400,
            title: 'Add resource',
            buttons: [
              {
                text: 'Create',
                'class': 'ok',
                click: function() {
                  if (!completeAction($addResource.find('.form-container'))) {
                    return false;
                  }

                  $('div.overlay').remove();
                  $(this).dialog('destroy');

                  return true;
                }
              },
              {
                text: 'Cancel',
                'class': 'cancel',
                click: function() {
                  $('div.overlay').remove();
                  $(this).dialog('destroy');
                }
              }
            ]
          }).closest('.ui-dialog').overlay();
          $select.trigger('change');

          return false;
        });

        return $chartView;
      };

      // Iterate through networks; render tabs
      var loadNetworkData = function() {
        // Toolbar
        var $toolbar = $('<div>').addClass('toolbar').appendTo($charts);
        var $refresh = $('<div>').addClass('button refresh').appendTo($toolbar)
              .append($('<span>').html('Refresh'));

        // Tab content
        var $tabMain = $('<div>').addClass('network-tabs').appendTo($charts);
        var $loading = $('<div>').addClass('loading-overlay').appendTo($tabMain);
        naas.networks.dataProvider({
          context: context,
          response: {
            success: function(args) {
              var $tabs = $('<ul>').appendTo($tabMain);

              $loading.remove();

              // Populate network data with individual zone charts
              $(args.data).each(function() {
                var tabID = 'tab-system-networks-' + this.id;
                var $tab = $('<li>').appendTo($tabs).append(
                  $('<a>')
                    .attr({
                      href: '#' + tabID
                    })
                    .html(this.name)
                );
                var $tabContent = $('<div>').appendTo($tabMain)
                      .attr({ id: tabID })
                      .append(chartView(this));

                // Tooltip hover event
                var $tooltip = $tabContent.find('.tooltip-info:visible').hide();
                $tabContent.find('li.main').mouseenter(function(event) {
                  $tooltip.css({ opacity: 0 });
                  $tooltip.show().animate({ opacity: 1 }, { queue: false });

                  var $item = $(this);

                  $item.siblings().each(function() {
                    $tooltip.removeClass($(this).attr('rel'));
                  });
                  $tooltip.addClass('tooltip-info ' + $item.attr('rel'));
                });

                $tabContent.find('li.main').mouseleave(function(event) {
                  $tooltip.animate({ opacity: 0 }, { queue: false });
                });

                // Main items configure event
                $tabContent.find('li.main .view-all.configure').click(function() {
                  var itemID = $(this).closest('li').attr('rel');

                  $browser.cloudBrowser('addPanel', {
                    title: itemID + ' details',
                    maximizeIfSelected: true,
                    complete: function($newPanel) {
                      $newPanel.detailView(
                        $.extend(true, {}, naas.mainNetworks[itemID].detailView, {
                          $browser: listViewArgs.$browser,
                          context: { zones: context.physicalResources }
                        })
                      );
                    }
                  });

                  return false;
                });

                // Provider configure event
                $tabContent.find('li.provider .view-all.configure').click(function() {
                  var $li = $(this).closest('li');
                  var itemID = $li.attr('rel');
                  var status = $li.attr('network-status');
                  var networkProviderArgs = naas.networkProviders.types[itemID];
                  var action = networkProviderArgs.actions ? networkProviderArgs.actions.add : null;
                  var createForm = action ? networkProviderArgs.actions.add.createForm : null;
                  var itemName = networkProviderArgs.label;

                  /**
                   * Generate provider-wide actions
                   */
                  var loadProviderActions = function($listView) {
                    $listView.find('.toolbar .main-actions').remove();

                    var $providerActions = providerActions(
                      networkProviderArgs.providerActions ?
                        networkProviderArgs.providerActions : {},
                      {
                        success: function(args) {
                          var action = args.action;
                          $loading.appendTo($listView);

                          $('div.notifications').notifications('add', {
                            desc: action.messages.notification({}),
                            interval: 2000,
                            poll: action.notification.poll,
                            _custom: args ? args._custom : null,
                            complete: function(args) {
                              $loading.remove();
                              loadProviderActions($listView);
                            }
                          });
                        },

                        actionFilter: networkProviderArgs.providerActionFilter
                      }
                    );

                    $providerActions.appendTo($listView.find('.toolbar'));
                  };

                  $browser.cloudBrowser('addPanel', {
                    title: itemName + ' details',
                    maximizeIfSelected: true,
                    complete: function($newPanel) {
                      if (status == 'not-configured') {
                        // Create form
                        var formData = cloudStack.dialog.createForm({
                          form: createForm,
                          context: { zones: listViewArgs.context.physicalResources },
                          after: function(args) {
                            action.action($.extend(args, {
                              response: {
                                success: function(args) {
                                  $newPanel.find('form').prepend($('<div>').addClass('loading-overlay'));
                                  $('div.notifications').notifications('add', {
                                    desc: action.messages.notification({}),
                                    interval: 1000,
                                    poll: action.notification.poll,
                                    _custom: args ? args._custom : null,
                                    complete: function(args) {
                                      refreshChart();
                                      var $listView = $newPanel.html('').listView({
                                        listView: naas.networkProviders.types[itemID]
                                      });

                                      loadProviderActions($listView);
                                    }
                                  });
                                }
                              }
                            }));
                          },
                          noDialog: true
                        });

                        var $formContainer = formData.$formContainer.addClass('add-first-network-resource');
                        var $form = $formContainer.find('form');
                        var completeAction = formData.completeAction;

                        $newPanel.append(
                          $formContainer
                            .prepend(
                              $('<div>').addClass('title').html('Add new ' + itemName + ' device')
                            )
                            .append(
                              $('<div>')
                                .addClass('button submit')
                                .append($('<span>').html('Add'))
                                .click(function() {
                                  if ($form.valid()) {
                                    completeAction($formContainer);
                                  }
                                })
                            )
                        );
                      } else {
                        var provider = naas.networkProviders.types[itemID];

                        if (provider.type == 'detailView') {
                          var $detailView = $newPanel.detailView(provider);
                        } else {
                          var $listView = $newPanel.listView({
                            listView: provider
                          });

                          loadProviderActions($listView); 
                        }
                      }
                    }
                  });
                });
              });

              $tabMain.tabs();
              $tabMain.find('li:first').addClass('first');
              $tabMain.find('li:last').addClass('last');
            }
          }
        });

        var refreshChart = function() {
          $charts.children().remove();
          loadNetworkData();
        };

        $refresh.click(function() {
          refreshChart();
          return false;
        });

        $(window).bind('cloudStack.fullRefresh', function(event) {
          refreshChart();
        });
      };

      loadNetworkData();

      return $charts;
    };
  };
})(jQuery, cloudStack);
