(function($, cloudStack) {
  /**
   * Add 'pending' notification
   */
  var addNotification = function(notification, success, successArgs, error, errorArgs) {
    if (!notification) {
      success(successArgs);

      return false;
    };

    var $notifications = $('div.notifications');

    if (!notification.poll) {
      $notifications.notifications('add', {
        section: notification.section,
        desc: notification.desc,
        interval: 0,
        poll: function(args) { success(successArgs); args.complete(); }
      });
    } else {
      $notifications.notifications('add', {
        section: notification.section,
        desc: notification.desc,
        interval: 5000,
        _custom: notification._custom,
        poll: function(args) {
          var complete = args.complete;

          notification.poll({
            _custom: args._custom,
            complete: function(args) {
              success($.extend(successArgs, args));
              complete(args);
            },
            error: function(args) {
              if (args.message) {
                if (args.message) {
                  cloudStack.dialog.notice({ message: args.message });
                }

                error($.extend(errorArgs, args));
                complete(args);
              }
            }
          });
        }
      });
    }

    return true;
  };

  /**
   * Available UI actions to perform for buttons
   */
  var uiActions = {
    /**
     * Default behavior for actions -- just show a confirmation popup and add notification
     */
    standard: function($detailView, args, additional) {
      var action = args.actions[args.actionName];
      var notification = action.notification;
      var messages = action.messages;
      var messageArgs = { name: $detailView.find('tr.name td.value').html() };
      var id = args.id;
      var context = $detailView.data('view-args').context;

      var externalLinkAction = action.action.externalLink;
      if (externalLinkAction) {
        // Show popup immediately, do not proceed through normal action process
        window.open(
          // URL
          externalLinkAction.url({
            context: context
          }),

          // Title
          externalLinkAction.title({
            context: context
          }),

          // Window options
          'menubar=0,resizable=0,'
            + 'width=' + externalLinkAction.width + ','
            + 'height=' + externalLinkAction.height
        );
        
        return;
      }

      notification.desc = messages.notification(messageArgs);
      notification.section = 'instances';

      var performAction = function(data, options) {
        if (!options) options = {};

        var $form = options.$form;

        action.action({
          data: data,
          ref: options.ref,
          context: $detailView.data('view-args').context,
          $form: $form,
          response: {
            success: function(args) {
              args = args ? args : {};
              notification._custom = args._custom;

              // Set loading appearance
              $detailView.prepend(
                $('<div>').addClass('loading-overlay')
              );

              if (additional && additional.success) additional.success(args);

              // Setup notification
              addNotification(
                notification,
                function(args) {
                  if ($detailView.is(':visible')) {
                    $detailView.find('.loading-overlay').remove();

                    // Refresh actions
                    loadTabContent(
                      $detailView.find('.detail-group:visible'),
                      $detailView.data('view-args')
                    );
                  }

                  if (messages.complete) {
                    cloudStack.dialog.notice({
                      message: messages.complete(args.data)
                    });
                  }
                  if (additional && additional.complete) additional.complete($.extend(true, args, {
                    $detailView: $detailView
                  }));
                },

                {},

                // Error
                function(args) {

                }
              );
            },
            error: function(args) {
              if (args.message)
                cloudStack.dialog.notice({ message: args.message });
            }
          }
        });
      };

      if (!action.createForm)
        cloudStack.dialog.confirm({
          message: messages.confirm(messageArgs),
          action: function() {
            performAction({
              id: id
            });
          }
        });
      else {
        cloudStack.dialog.createForm({
          form: action.createForm,
          after: function(args) {
            performAction(args.data, { 
              ref: args.ref, 
              context: $detailView.data('view-args').context,
              $form: args.$form
            });
          },
          ref: {
            id: id
          },
          context: $detailView.data('view-args').context
        });
      }
    },

    destroy: function($detailView, args) {
      uiActions.standard($detailView, args, {
        complete: function(args) {
          var $browser = $detailView.data('view-args').$browser;
          var $panel = $detailView.closest('.panel');

          $browser.cloudBrowser('selectPanel', {
            panel: $panel.prev()
          });
        }
      });
    },

    /**
     * Convert editable fields to text boxes; clicking again saves data
     *
     * @param $detailView
     * @param callback
     */
    edit: function($detailView, args) {
      // Convert value TDs
      var $inputs = $detailView.find('input[type=text], select');

      if ($inputs.size()) {
        $inputs.animate({ opacity: 0.5 }, 500);

        var data = {};
        $inputs.each(function() {
          data[$(this).attr('name')] = $(this).val();
        });

        args.actions[args.actionName].action({
          data: data,
          context: $detailView.data('view-args').context,
          response: {
            data: data,
            success: function(data) {
              // Save and turn back into labels
              $inputs.each(function() {
                var $input = $(this);
                var $value = $input.closest('td.value');

                if ($input.is('input[type=text]'))
                  $value.html(
                    $input.attr('value')
                  );
                else if ($input.is('select')) {
                  $value.html(
                    $input.find('option:selected').html()
                  );
                  $value.data('detail-view-selected-option', $input.find('option:selected').val());
                }
              });

              addNotification(
                {
                  section: $detailView.data('view-args').id,
                  desc: 'Renamed VM'
                },
                function(data) {
                },
                []
              );
            },
            error: function(args) {
              // Put in original values on error
              $inputs.each(function() {
                var $input = $(this);
                var $value = $input.closest('td.value');
                var originalValue = $input.data('original-value');

                $value.html(originalValue);
              });

              if (args.message) cloudStack.dialog.notice({ message: args.message });
            }
          }
        });

        return $detailView;
      }

      $detailView.find('td.value').each(function() {
        var name = $(this).closest('tr').data('detail-view-field');
        var $value = $(this);
        if (!$value.data('detail-view-is-editable')) return true;

        // Turn into form field
        var selectData = $value.data('detail-view-editable-select');

        if (selectData) {
          // Select
          var data = $value.html();
          $value.html('');
          $value.append(
            $('<select>')
              .attr({
                name: name,
                type: 'text',
                value: data
              })
              .data('original-value', data)
          );

          // Make option values from given array
          $(selectData).each(function() {
            $('<option>')
              .attr({
                value: this.id
              })
              .html(this.description)
              .appendTo($value.find('select'));
          });

          $value.find('select').val($value.data('detail-view-selected-option'));
        } else {
          // Text input
          var data = $value.html();
          $value.html('');
          $value.append(
            $('<input>').attr({
              name: name,
              type: 'text',
              value: data
            }).data('original-value', data)
          );
        }

        return true;
      });

      return $detailView;
    }
  };

  var viewAll = function(viewAllID) {
    var $detailView = $('div.detail-view:last');
    var args = $detailView.data('view-args');
    var cloudStackArgs = $('[cloudstack-container]').data('cloudStack-args');
    var $browser = args.$browser;
    var listViewArgs, viewAllPath;
    var $listView;

    // Get path in cloudStack args
    viewAllPath = viewAllID.split('.');

    if (viewAllPath.length == 2) {
      if (viewAllPath[0] != '_zone')
        listViewArgs = cloudStackArgs.sections[viewAllPath[0]].sections[viewAllPath[1]];
      else {
        // Sub-section of the zone chart
        listViewArgs = cloudStackArgs.sections.system
          .subsections[viewAllPath[1]];
      }
    }
    else
      listViewArgs = cloudStackArgs.sections[viewAllPath[0]];

    // Make list view
    listViewArgs.$browser = $browser;

    if (viewAllPath.length == 2)
      listViewArgs.id = viewAllPath[0];
    else
      listViewArgs.id = viewAllID;

    listViewArgs.ref = {
      id: args.id,
      type: $detailView.data('view-args').section
    };

    // Load context data
    var context = $.extend(true, {}, $detailView.data('view-args').context);

    // Make panel
    var $panel = $browser.cloudBrowser('addPanel', {
      title: listViewArgs.title,
      data: '',
      noSelectPanel: true,
      maximizeIfSelected: true,
      complete: function($newPanel) {
        return $('<div>').listView(listViewArgs, { context: context }).appendTo($newPanel);
      }
    });
  };

  /**
   * Make action button elements
   *
   * @param actions {object} Actions to generate
   */
  var makeActionButtons = function(actions, options) {
    options = options ? options : {};
    var $actions = $('<td>').addClass('detail-actions').append(
      $('<div>').addClass('buttons')
    );

    var allowedActions = [];

    if (actions) {
      allowedActions = $.map(actions, function(value, key) {
        return key;
      });

      if (options.actionFilter)
        allowedActions = options.actionFilter({
          context: $.extend(true, {}, options.context, {
            actions: allowedActions,
            item: options.data
          })
        });

      $.each(actions, function(key, value) {
        if ($.inArray(key, allowedActions) == -1) return true;

        var $action = $('<div></div>')
              .addClass('action').addClass(key)
              .appendTo($actions.find('div.buttons'));
        var $actionLink = $('<a></a>')
              .attr({
                href: '#',
                title: value.label,
                alt: value.label,
                'detail-action': key
              })
              .data('detail-view-action-callback', value.action)
              .append(
                $('<span>').addClass('icon').html('&nbsp;')
              )
              .appendTo($action);

        return true;
      });

      var $actionButtons = $actions.find('div.action');
      if ($actionButtons.size() == 1)
        $actionButtons.addClass('single');
      else {
        $actionButtons.filter(':first').addClass('first');
        $actionButtons.filter(':last').addClass('last');
      }
    }

    return $('<div>')
      .addClass('detail-group actions')
      .append(
        $('<table>').append(
          $('<tbody>').append(
            $('<tr>').append($actions)
          )
        )
      );
  };

  /**
   * Generate attribute field rows in tab
   */
  var makeFieldContent = function(tabData, $detailView, data, args) {
    if (!args) args = {};

    var $detailGroups = $('<div>').addClass('details');
    var isOddRow = false; // Even/odd row coloring
    var $header;
    var detailViewArgs = $detailView.data('view-args');
    var fields = tabData.fields;
    var hiddenFields;
    var context = detailViewArgs.context;
    var isMultiple = tabData.multiple || tabData.isMultiple;

    if (isMultiple) {
      context[tabData.id] = data;
    }

    // Make header
    if (args.header) {
      $detailGroups.addClass('group-multiple');
      $header = $('<table>').addClass('header').appendTo($detailGroups);
      $header.append($('<thead>').append($('<tr>')));
      $header.find('tr').append($('<th>'));
    }

    if (tabData.preFilter) {
      hiddenFields = tabData.preFilter({
        context: context,
        fields: $.map(fields, function(fieldGroup) { 
          return $.map(fieldGroup, function(value, key) { return key; }); 
        })
      });
    }

    $(fields).each(function() {
      var fieldGroup = this;

      var $detailTable = $('<tbody></tbody>').appendTo(
        $('<table></table>').appendTo(
          $('<div></div>').addClass('detail-group').appendTo($detailGroups)
        ));

      $.each(fieldGroup, function(key, value) {
        if (hiddenFields && $.inArray(key, hiddenFields) >= 0) return true;
        if ($header && key == args.header) {
          $header.find('th').html(data[key]);
          return true;
        }

        var $detail = $('<tr></tr>').addClass(key).appendTo($detailTable);
        var $name = $('<td></td>').addClass('name').appendTo($detail);
        var $value = $('<td></td>').addClass('value').appendTo($detail);
        var content = data[key];

        if (this.converter) content = this.converter(content);

        $detail.data('detail-view-field', key);

        // Even/odd row coloring
        if (isOddRow && key != 'name') {
          $detail.addClass('odd');
          isOddRow = false;
        } else if (key != 'name') {
          isOddRow = true;
        }

        $name.html(value.label);
        $value.html(content);

        // Set up editable metadata
        $value.data('detail-view-is-editable', value.isEditable);
        if (value.select) {
          value.selected = $value.html();

          value.select({
            response: {
              success: function(args) {
                // Get matching select data
                var matchedSelectValue = $.grep(args.data, function(option, index) {
                  return option.id == value.selected;
                })[0];

                if(matchedSelectValue != null) {
                  $value.html(matchedSelectValue.description);
                  $value.data('detail-view-selected-option', matchedSelectValue.id);
                }

                $value.data('detail-view-editable-select', args.data);

                return true;
              }
            }
          });
        }

        return true;
      });
    });

    if (args.isFirstPanel) {
      var $firstRow = $detailGroups.filter(':first').find('div.detail-group:first table tr:first');
      var $actions;
      var actions = detailViewArgs.actions;
      var actionFilter = args.actionFilter;

      // Detail view actions
      if (actions || detailViewArgs.viewAll)
        $actions = makeActionButtons(detailViewArgs.actions, {
          actionFilter: actionFilter,
          data: data,
          context: $detailView.data('view-args').context
        }).prependTo($firstRow.closest('div.detail-group'));

      // 'View all' button
      if (detailViewArgs.viewAll) {
        $('<div>')
          .addClass('view-all')
          .append(
            $('<a>')
              .attr({ href: '#' })
              .data('detail-view-link-view-all', detailViewArgs.viewAll)
              .append(
                $('<span>').html('View ' + detailViewArgs.viewAll.label)
              )
          )
          .append(
            $('<div>').addClass('end')
          )
          .appendTo(
            $('<td>')
              .addClass('view-all')
              .appendTo($actions.find('tr'))
          );

      }
    }

    return $detailGroups;
  };

  /**
   * Load field data for specific tab from data provider
   *
   * @param $tabContent {jQuery} tab div to load content into
   * @param args {object} Detail view data
   */
  var loadTabContent = function($tabContent, args) {
    $tabContent.html('');

    var targetTabID = $tabContent.data('detail-view-tab-id');
    var tabs = args.tabs[targetTabID];
    var dataProvider = tabs.dataProvider;
    var isMultiple = tabs.multiple || tabs.isMultiple;
    var viewAll = args.viewAll;
    var $detailView = $tabContent.closest('.detail-view');

    if (tabs.custom) {
      return tabs.custom({
        context: args.context
      }).appendTo($tabContent);
    }

    $.extend(
      $detailView.data('view-args'),
      { activeTab: targetTabID }
    );

    $tabContent.append(
      $('<div>').addClass('loading-overlay')
    );

    return dataProvider({
      tab: targetTabID,
      id: args.id,
      jsonObj: args.jsonObj,
      context: args.context,
      response: {
        success: function(args) {
          var tabData = $tabContent.data('detail-view-tab-data');
          var data = args.data;
          var isFirstPanel = $tabContent.index($detailView.find('div.detail-group.ui-tabs-panel')) == 0;
          var actionFilter = args.actionFilter;

          $tabContent.find('.loading-overlay').remove();

          if (isMultiple) {
            $(data).each(function() {
              var $fieldContent = makeFieldContent(
                $.extend(true, {}, tabs, {
                  id: targetTabID
                }),
                $tabContent.closest('div.detail-view'), this, {
                  header: 'name',
                  isFirstPanel: isFirstPanel,
                  actionFilter: actionFilter
                }
              ).appendTo($tabContent);
            });

            return true;
          }

          makeFieldContent(tabs, $tabContent.closest('div.detail-view'), data, {
            isFirstPanel: isFirstPanel,
            actionFilter: actionFilter
          }).appendTo($tabContent);

          return true;
        },
        error: function() {
          alert('error!');
        }
      }
    });
  };

  $.fn.detailView = function(args) {
    var $detailView = this;

    $detailView.addClass('detail-view');
    $detailView.data('view-args', args);

    // Create toolbar
    var $toolbar = $('<div class="toolbar">')
          .append(
            $('<div>')
              .addClass('button refresh')
              .append(
                $('<span>').html('Refresh')
              )
          )
          .appendTo($detailView);
    var $tabs = $('<ul></ul>').appendTo($detailView);

    // Get tab filter, if present
    var removedTabs = [];
    if (args.tabFilter) {
      removedTabs = args.tabFilter({
        context: args.context
      });
    }

    // Make tabs
    $.each(args.tabs, function(key, value) {
      // Don't render tab, if filtered out
      if ($.inArray(key, removedTabs) > -1) return true;

      var propGroup = key;
      var prop = value;
      var title = prop.title;
      var $tab = $('<li></li>').attr('detail-view-tab', true).appendTo($tabs);

      var $tabLink = $('<a></a>').attr({
        href: '#details-tab-' + propGroup
      }).html(title).appendTo($tab);

      var $tabContent = $('<div></div>').attr({
        id: 'details-tab-' + propGroup
      }).addClass('detail-group').appendTo($detailView);

      $tabContent.data('detail-view-tab-id', key);
      $tabContent.data('detail-view-tab-data', value);
    });

    $tabs.find('li:first').addClass('first');
    $tabs.find('li:last').addClass('last');

    $detailView.tabs();

    return $detailView;
  };

  // Setup tab events
  $(document).bind('tabsshow', function(event, ui) {
    var $target = $(event.target);

    if (!$target.hasClass('detail-view') || $target.hasClass('detail-view ui-state-active')) return true;

    var $targetDetailGroup = $(ui.panel);
    loadTabContent($targetDetailGroup, $target.data('view-args'));

    return true;
  });

  // View all links
  $('a').live('click', function(event) {
    var $target = $(event.target);
    var $viewAll = $target.closest('td.view-all a');

    if ($target.closest('div.detail-view').size() && $target.closest('td.view-all a').size()) {
      viewAll($viewAll.data('detail-view-link-view-all').path);
      return false;
    }

    return true;
  });

  // Setup view events
  $(window).bind('cloudstack.view.details.remove', function(event, data) {
    var $detailView = data.view;
    $('#browser .container').cloudBrowser('selectPanel', {
      panel: $detailView.closest('div.panel').prev()
    });
  });

  // Setup action button events
  $(document).bind('click', function(event) {
    var $target = $(event.target);

    // Refresh
    if ($target.closest('div.toolbar div.refresh').size()) {
      loadTabContent(
        $target.closest('div.detail-view').find('div.detail-group:visible'),
        $target.closest('div.detail-view').data('view-args')
      );

      return false;
    }

    // Detail action
    if ($target.closest('div.detail-view [detail-action]').size()) {
      var $action = $target.closest('div.detail-view [detail-action]');
      var actionName = $action.attr('detail-action');
      var actionCallback = $action.data('detail-view-action-callback');
      var detailViewArgs = $action.closest('div.detail-view').data('view-args');
      var additionalArgs = {};
      var actionSet = uiActions;

      var uiCallback = actionSet[actionName];
      if (!uiCallback)
        uiCallback = actionSet['standard'];

      detailViewArgs.actionName = actionName;

      uiCallback($target.closest('div.detail-view'), detailViewArgs, additionalArgs);

      return false;
    }

    return true;
  });
}(jQuery, cloudStack));
