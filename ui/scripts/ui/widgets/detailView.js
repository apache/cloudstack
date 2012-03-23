(function($, cloudStack, _l) {
  var replaceListViewItem = function($detailView, newData) {
    var $row = $detailView.data('list-view-row');

    if (!$row) return;

    var $listView = $row.closest('.list-view');
    var $newRow;
    var jsonObj = $row.data('json-obj');

		if($listView.length > 0 ) { //$listView.length is 0 after calling $(window).trigger('cloudStack.fullRefresh')
			$listView.listView('replaceItem', {
				$row: $row,
				data: $.extend(jsonObj, newData),
				after: function($newRow) {
					$detailView.data('list-view-row', $newRow);

					setTimeout(function() {
						$('.data-table').dataTable('selectRow', $newRow.index());
					}, 100);
				}
			});
    }
		
    // Refresh detail view context
    $.extend(
      $detailView.data('view-args').context[
        $detailView.data('view-args').section
      ][0], newData
    );
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
      var preAction = action.preAction;
      var notification = action.notification ?
            action.notification : {};
      var messages = action.messages;
      var messageArgs = { name: $detailView.find('tr.name td.value').html() };
      var id = args.id;
      var context = $detailView.data('view-args').context;
      var _custom = $detailView.data('_custom');
      var customAction = action.action.custom;
      var noAdd = action.noAdd;
      var noRefresh = additional.noRefresh;

      // Handle pre-action (occurs before any other behavior happens)
      if (preAction) {
        if (!preAction({ context: context })) return false;
      }

      var updateTabContent = function(newData) {
        var $detailViewElems = $detailView.find('ul.ui-tabs-nav, .detail-group').remove();
        $detailView.tabs('destroy');
        $detailView.data('view-args').jsonObj = newData;

        makeTabs(
          $detailView,
          $detailView.data('view-args').tabs,
          {
            context: context,
            tabFilter: $detailView.data('view-args').tabFilter,
            newData: newData
          }
        ).appendTo($detailView);

        $detailView.tabs();
      };

      var performAction = function(data, options) {
        if (!options) options = {};

        var $form = options.$form;

        if (customAction && !noAdd) {
          customAction({
            context: context,
            $detailView: $detailView,
            complete: function(args) {
              // Set loading appearance
              var $loading = $('<div>').addClass('loading-overlay');

              $detailView.prepend($loading);

              args = args ? args : {};

              var $item = args.$item;

              notification.desc = messages.notification(args.messageArgs);
              notification._custom = $.extend(args._custom ? args._custom : {}, {
                $detailView: $detailView
              });

              cloudStack.ui.notifications.add(
                notification,

                // Success
                function(args) {
                  if (!$detailView.is(':visible')) return;

                  $loading.remove();
                  replaceListViewItem($detailView, args.data);

                  if (!noRefresh) {
                    updateTabContent(args.data);
                  }
                },

                {},

                // Error
                function(args) {
                  $loading.remove();
                }
              );
            }
          });
        } else {
          // Set loading appearance
          var $loading = $('<div>').addClass('loading-overlay');
          $detailView.prepend($loading);

          action.action({
            data: data,
            _custom: _custom,
            ref: options.ref,
            context: $detailView.data('view-args').context,
            $form: $form,
            response: {
              success: function(args) {
                args = args ? args : {};
                notification._custom = $.extend(args._custom ? args._custom : {}, {
                  $detailView: $detailView
                });

                if (additional && additional.success) additional.success(args);

                // Setup notification
                cloudStack.ui.notifications.add(
                  notification,
                  function(args) {
                    if ($detailView.is(':visible')) {
                      $loading.remove();

                      if (!noRefresh) {
                        updateTabContent(args.data);
                      }
                    }

                    if (messages.complete) {
                      cloudStack.dialog.notice({
                        message: messages.complete(args.data)
                      });
                    }
                    if (additional && additional.complete) additional.complete($.extend(true, args, {
                      $detailView: $detailView
                    }));

                    replaceListViewItem($detailView, args.data);
                  },

                  {},

                  // Error
                  function(args) {
                    $loading.remove();
                  }
                );

                return true;
              },
              error: function(args) {		//args here is parsed errortext from API response	
							  if(args != null & args.length > 0) {
									cloudStack.dialog.notice({
										message: args
									});			
								}
                $loading.remove();
              }
            }
          });
        }
      };

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
      } else {
        notification.desc = messages.notification(messageArgs);
        notification.section = 'instances';

        if (!action.createForm) {
          if (messages && messages.confirm) {
            cloudStack.dialog.confirm({
              message: messages.confirm(messageArgs),
              action: function() {
                performAction({
                  id: id
                });
              }
            });
          } else {
            performAction({ id: id });
          }
         } else {
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
      }
    },

    remove: function($detailView, args) {
      uiActions.standard($detailView, args, {
        noRefresh: true,
        complete: function(args) {
          var $browser = $('#browser .container');
          var $panel = $detailView.closest('.panel');

          if ($detailView.is(':visible')) {
            $browser.cloudBrowser('selectPanel', {
              panel: $panel.prev()
            });
          }
										
					if($detailView.data("list-view-row") != null) {					 
					  $detailView.data("list-view-row").remove();
					}					
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
      if ($detailView.find('.button.done').size()) return false;

      // Convert value TDs
      var $inputs = $detailView.find('input, select');
      var action = args.actions[args.actionName];
      var id = $detailView.data('view-args').id;
      var $editButton = $('<div>').addClass('button done').html(_l('label.apply')).hide();
      var $cancelButton = $('<div>').addClass('button cancel').html(_l('label.cancel')).hide();

      // Show buttons
      $.merge($editButton, $cancelButton)
        .appendTo(
          $detailView.find('.ui-tabs-panel .detail-group.actions')              
        ).fadeIn();

      var convertInputs = function($inputs) {
        // Save and turn back into labels
        $inputs.each(function() {
          var $input = $(this);
          var $value = $input.closest('td.value');

          if ($input.is('input[type=text]'))
            $value.html(_s(
              $input.attr('value')
            ));
          else if ($input.is('input[type=checkbox]')) {
            var val = $input.is(':checked');
            
            $value.data('detail-view-boolean-value', _s(val));
            $value.html(_s(val) ? _l('label.yes') : _l('label.no'));
          }
          else if ($input.is('select')) {
            $value.html(_s(
              $input.find('option:selected').html()
            ));
            $value.data('detail-view-selected-option', _s($input.find('option:selected').val()));
          }
        });
      };

      // Put in original values
      var cancelEdits = function($inputs, $editButton) {
        $inputs.each(function() {
          var $input = $(this);
          var $value = $input.closest('td.value');
          var originalValue = $input.data('original-value');

          $value.html(_s(originalValue));
        }); 

        $editButton.fadeOut('fast', function() {
          $editButton.remove();
        });  
      };

      var applyEdits = function($inputs, $editButton) {
        if ($inputs.size()) {
          $inputs.animate({ opacity: 0.5 }, 500);

          var data = {};
          $inputs.each(function() {
            var $input = $(this);

            if ($input.is('[type=checkbox]')) {
              data[$input.attr('name')] = $input.is(':checked') ? 'on' : null;
            } else {
              data[$input.attr('name')] = $input.val(); 
            }
          });

          $editButton.fadeOut('fast', function() {
            $editButton.remove();
          });

          var $loading = $('<div>').addClass('loading-overlay');

          action.action({
            data: data,
            _custom: $detailView.data('_custom'),
            context: $detailView.data('view-args').context,
            response: {
              success: function(args) {
                var notificationArgs = {
                  section: id,
                  desc: _l('changed.item.properties'),
                  _custom: args ? args._custom : null
                };

                if (!action.notification) {
                  convertInputs($inputs);
                  cloudStack.ui.notifications.add(
                    notificationArgs, function() {}, []
                  );
                  replaceListViewItem($detailView, data);
                } else {
                  $loading.appendTo($detailView);
                  cloudStack.ui.notifications.add(
                    $.extend(true, {}, action.notification, notificationArgs),
                    function(args) {
                      replaceListViewItem($detailView, data);

                      convertInputs($inputs);
                      $loading.remove();
                    }, [],
                    function() {
                      $loading.remove();
                      $inputs.closest('.detail-view').find('.toolbar .refresh').click();
                    }, []
                  );
                }
              },
              error: function(message) {
                cancelEdits($inputs, $editButton);
                if (message) cloudStack.dialog.notice({ message: message });
              }
            }
          });
        }
      };

      $editButton.click(function() {
        var $inputs = $detailView.find('input, select');

        if ($(this).hasClass('done')) {
          applyEdits($inputs, $editButton);
        } else { // Cancel
          cancelEdits($inputs, $editButton);
        }
      });

      $detailView.find('td.value').each(function() {
        var name = $(this).closest('tr').data('detail-view-field');
        var $value = $(this);
        if (!$value.data('detail-view-is-editable')) return true;

        // Turn into form field
        var selectData = $value.data('detail-view-editable-select');
        var isBoolean = $value.data('detail-view-editable-boolean');
        var data = !isBoolean ? cloudStack.sanitizeReverse($value.html()) : $value.data('detail-view-boolean-value');

        $value.html('');

        if (selectData) {
          // Select
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
                value: _s(this.id)
              })
              .html(_s(this.description))
              .appendTo($value.find('select'));
          });

          $value.find('select').val($value.data('detail-view-selected-option'));
        } else if (isBoolean) {
          $value.append(
            $('<input>').attr({
              name: name,
              type: 'checkbox',
              checked: data
            })
          );
        } else {
          // Text input
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
    var isCustom = $.isFunction(viewAllID.custom);

    if (isCustom) {
      $browser.cloudBrowser('addPanel', {
        title: _l(viewAllID.label),
        maximizeIfSelected: true,
        complete: function($newPanel) {
          $newPanel.append(
            viewAllID.custom({
              $browser: $browser,
              context: $detailView.data('view-args').context,
              listViewArgs: $detailView.data('list-view') ?
                $detailView.data('list-view').data('view-args') : null
            })
          );
        }
      });

      return;
    }

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
      title: _l(listViewArgs.title),
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
                title: _l(value.label),
                alt: _l(value.label),
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
    var context = detailViewArgs ? detailViewArgs.context : cloudStack.context;
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

    $detailGroups.append($('<div>').addClass('main-groups'));

    $(fields).each(function() {
      var fieldGroup = this;

      var $detailTable = $('<tbody></tbody>').appendTo(
        $('<table></table>').appendTo(
          $('<div></div>').addClass('detail-group').appendTo($detailGroups.find('.main-groups'))
        ));

      $.each(fieldGroup, function(key, value) {
        if (hiddenFields && $.inArray(key, hiddenFields) >= 0) return true;
        if ($header && key == args.header) {
          $header.find('th').html(_s(data[key]));
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

        $name.html(_l(value.label));
        $value.html(_s(content));

        // Set up editable metadata
        $value.data('detail-view-is-editable', value.isEditable);
        if (value.select) {
          value.selected = $value.html();

          value.select({
            context: context,
            response: {
              success: function(args) {
                // Get matching select data
                var matchedSelectValue = $.grep(args.data, function(option, index) {
                  return option.id == value.selected;
                })[0];

                if(matchedSelectValue != null) {
                  $value.html(_s(matchedSelectValue.description));
                  $value.data('detail-view-selected-option', matchedSelectValue.id);
                }

                $value.data('detail-view-editable-select', args.data);

                return true;
              }
            }
          });
        } else if (value.isBoolean) {
          $value.data('detail-view-editable-boolean', true);
          $value.data('detail-view-boolean-value', content == 'Yes' ? true : false);
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
        }).prependTo($firstRow.closest('div.detail-group').closest('.details'));

      // 'View all' button
      var showViewAll = detailViewArgs.viewAll ?
            (
              detailViewArgs.viewAll.preFilter ?
                detailViewArgs.viewAll.preFilter({
                  context: context
                }) : true
            ) : true;
      if (detailViewArgs.viewAll && showViewAll) {
        $('<div>')
          .addClass('view-all')
          .append(
            $('<a>')
              .attr({ href: '#' })
              .data('detail-view-link-view-all', detailViewArgs.viewAll)
              .append(
                $('<span>').html(_l('label.view') + ' ' + _l(detailViewArgs.viewAll.label))
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
   * @param options {object} Additional options
   */
  var loadTabContent = function($tabContent, args, options) {
    if (!options) options = {};
    $tabContent.html('');

    var targetTabID = $tabContent.data('detail-view-tab-id');
    var tabs = args.tabs[targetTabID];
    var dataProvider = tabs.dataProvider;
    var isMultiple = tabs.multiple || tabs.isMultiple;
    var viewAll = args.viewAll;
    var $detailView = $tabContent.closest('.detail-view');
    var jsonObj = $detailView.data('view-args').jsonObj;

    if (tabs.custom) {
      return tabs.custom({
        context: args.context
      }).appendTo($tabContent);
    }

    if (tabs.listView) {
      return $('<div>').listView({
        context: args.context,
        listView: tabs.listView
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
      jsonObj: jsonObj,
      context: args.context,
      response: {
        success: function(args) {				
          if (options.newData) {
            $.extend(args.data, options.newData);
          }

          if (args._custom) {
            $detailView.data('_custom', args._custom);
          }
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

  var makeTabs = function($detailView, tabs, options) {
    if (!options) options = {};

    var $tabs = $('<ul>');
    var $tabContentGroup = $('<div>');
    var removedTabs = [];
    var tabFilter = options.tabFilter;
    var context = options.context ? options.context : {};

    if (options.newData) {
      $.extend(
        context[$detailView.data('view-args').section][0],
        options.newData
      );
    }

    if (tabFilter) {
      removedTabs = tabFilter({
        context: context
      });
    }

    $.each(tabs, function(key, value) {
      // Don't render tab, if filtered out
      if ($.inArray(key, removedTabs) > -1) return true;

      var propGroup = key;
      var prop = value;
      var title = prop.title;
      var $tab = $('<li>').attr('detail-view-tab', true).appendTo($tabs);

      var $tabLink = $('<a></a>').attr({
        href: '#details-tab-' + propGroup
      }).html(_l(title)).appendTo($tab);

      var $tabContent = $('<div>').attr({
        id: 'details-tab-' + propGroup
      }).addClass('detail-group').appendTo($tabContentGroup);

      $tabContent.data('detail-view-tab-id', key);
      $tabContent.data('detail-view-tab-data', value);

      return true;
    });

    $tabs.find('li:first').addClass('first');
    $tabs.find('li:last').addClass('last');

    return $.merge(
      $tabs, $tabContentGroup.children()
    );
  };

  var replaceTabs = function($detailView, $newTabs, tabs, options) {
    var $detailViewElems = $detailView.find('ul.ui-tabs-nav, .detail-group');
    $detailView.tabs('destroy');
    $detailViewElems.remove();

    makeTabs($detailView, tabs, options).appendTo($detailView);
  };

  var makeToolbar = function() {
    return $('<div class="toolbar">')
      .append(
        $('<div>')
          .addClass('button refresh')
          .append(
            $('<span>').html(_l('label.refresh'))
          )
      );
  };

  $.fn.detailView = function(args) {
    var $detailView = this;

    $detailView.addClass('detail-view');
    $detailView.data('view-args', args);

    if (args.$listViewRow) {
      $detailView.data('list-view-row', args.$listViewRow);
    }

    // Create toolbar
    var $toolbar = makeToolbar().appendTo($detailView);

    // Create tabs
    var $tabs = makeTabs($detailView, args.tabs, {
      context: args.context,
      tabFilter: args.tabFilter
    }).appendTo($detailView);

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
      viewAll(
        $viewAll.data('detail-view-link-view-all').custom ?
          $viewAll.data('detail-view-link-view-all') :
          $viewAll.data('detail-view-link-view-all').path
      );
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
}(window.jQuery, window.cloudStack, window._l));
