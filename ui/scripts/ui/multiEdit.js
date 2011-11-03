(function($, cloudStack) {
  var _medit = cloudStack.ui.widgets.multiEdit = {
    /**
     * Append item to list
     */
    addItem: function(data, fields, $multi, itemData, actions, options) {
      if (!options) options = {};

      var $item = $('<div>').addClass('data-item').append(
        $('<table>').append($('<tbody>'))
      );
      var $tr = $('<tr>').appendTo($item.find('tbody'));

      if (itemData) {
        $tr.data('multi-edit-data', itemData);
      }

      // Setup columns
      $.each(fields, function(fieldName, field) {
        if (options.ignoreEmptyFields && !data[fieldName]) {
          return true;
        }

        var $td = $('<td>').addClass(fieldName).appendTo($tr);
        var $input, val;

        if ($multi.find('th,td').filter(function() {
          return $(this).attr('rel') == fieldName;
        }).is(':hidden')) return true;

        if (!field.isPassword) {
          if (field.edit) {
            // Edit fields append value of data
            if (field.range) {
              var start = data[field.range[0]];
              var end = data[field.range[1]];

              $td.append(
                $('<span>').html(start + ' - ' + end)
              );
            } else {
              $td.append(
                $('<span>').html(data[fieldName])
              );
            }
          } else if (field.select) {
            $td.append(
              $('<span>').html(
                // Get matching option text
                $multi.find('select').filter(function() {
                  return $(this).attr('name') == fieldName;
                }).find('option').filter(function() {
                  return $(this).val() == data[fieldName];
                }).html()
              )
            );
          } else if (field.addButton && $.isArray(itemData) && !options.noSelect) {
            // Show VM data
            $td
              .html(
                options.multipleAdd ?
                  itemData.length + ' VMs' : itemData[0].name
              )
              .click(function() {
                var $browser = $(this).closest('.detail-view').data('view-args').$browser;

                if (options.multipleAdd) {
                  _medit.multiItem.details(itemData, $browser);
                } else {
                  _medit.details(itemData[0], $browser, { context: options.context });
                }
              });
          }
        };

        // Add blank styling for empty fields
        if ($td.html() == '') {
          $td.addClass('blank');
        }

        // Align width to main header
        var targetWidth = $multi.find('th.' + fieldName).width() + 5;
        $td.width(targetWidth);

        return true;
      });

      // Actions column
      var $actions = $('<td>').addClass('multi-actions').appendTo(
        $item.find('tr')
      );

      // Align action column width
      $actions.width(
        $multi.find('th.multi-actions').width() + 4
      );

      // Action filter
      var allowedActions = options.preFilter ? options.preFilter({
        context: $.extend(true, {}, options.context, {
          actions: $.map(actions, function(value, key) { return key; })
        })
      }) : null;

      // Append actions
      $.each(actions, function(actionID, action) {
        if (allowedActions && $.inArray(actionID, allowedActions) == -1) return true;

        $actions.append(
          $('<div>')
            .addClass('action')
            .addClass(actionID)
            .append($('<span>').addClass('icon'))
            .attr({ title: action.label })
            .click(function() {
              var $target = $(this);
              var $dataItem = $target.closest('.data-item');
              var $expandable = $dataItem.find('.expandable-listing');
              var isDestroy = $target.hasClass('destroy');

              if (isDestroy) {
                var $loading = _medit.loadingItem($multi, 'Removing...');

                if ($expandable.is(':visible')) {
                  $expandable.slideToggle(function() {
                    $dataItem.replaceWith($loading);
                  });
                } else {
                  // Loading appearance
                  $dataItem.replaceWith($loading);
                }
              }

              action.action({
                context: $.extend(true, {}, options.context, {
                  multiRule: [data]
                }),
                response: {
                  success: function(args) {
                    var notification = args.notification;
                    var _custom = args._custom;
                    if (notification) {
                      $('.notifications').notifications('add', {
                        section: 'network',
                        desc: notification.label,
                        interval: 500,
                        _custom: _custom,
                        poll: function(args) {
                          var complete = args.complete;

                          notification.poll({
                            _custom: args._custom,
                            complete: function(args) {
                              if (isDestroy) {
                                $loading.remove();
                              }

                              complete();
                            }
                          });
                        }
                      });
                    }
                  }
                }
              });
            })
        );
      });

      // Add expandable listing, for multiple-item
      if (options.multipleAdd) {
        // Create expandable box
        _medit.multiItem.expandable(
          $item.find('tr').data('multi-edit-data')
        ).appendTo($item);

        // Expandable icon/action
        $item.find('td:first').prepend(
          $('<div>')
            .addClass('expand')
            .click(function() {
              $item.closest('.data-item').find('.expandable-listing').slideToggle();
            })
        );
      }

      return $item;
    },

    /**
     * Align width of each data row to main header
     */
    refreshItemWidths: function($multi) {
      $multi.find('.data tr').filter(function() {
        return !$(this).closest('.expandable-listing').size();
      }).each(function() {
        var $tr = $(this);
        $tr.find('td').each(function() {
          var $td = $(this);

          $td.width(
            $(
              $multi.find('th:visible')[
                $td.index()
              ]
            ).width() + 5
          );
        });
      });
    },

    /**
     * Create a fake 'loading' item box
     */
    loadingItem: function($multi, label) {
      var $loading = $('<div>').addClass('data-item loading');

      // Align height with existing items
      var $row = $multi.find('.data-item:first');

      // Set label
      if (label) {
        $loading.append(
          $('<div>').addClass('label').append(
            $('<span>').html(label)
          )
        );
      }

      return $loading;
    },
    details: function(data, $browser, options) {
      if (!options) options = {};

      var detailViewArgs, $detailView;

      detailViewArgs = $.extend(true, {}, cloudStack.sections.instances.listView.detailView);
      detailViewArgs.actions = null;
      detailViewArgs.$browser = $browser;
      detailViewArgs.id = data.id;
      detailViewArgs.jsonObj = data[0];
      detailViewArgs.context = options.context;

      $browser.cloudBrowser('addPanel', {
        title: data.name,
        complete: function($newPanel) {
          $newPanel.detailView(detailViewArgs);
        }
      });
    },
    multiItem: {
      /**
       * Show listing of load balanced VMs
       */
      details: function(data, $browser) {
        var listViewArgs, $listView;

        // Setup list view
        listViewArgs = $.extend(true, {}, cloudStack.sections.instances);
        listViewArgs.listView.actions = null;
        listViewArgs.listView.filters = null;
        listViewArgs.$browser = $browser;
        listViewArgs.listView.detailView.actions = null;
        listViewArgs.listView.dataProvider = function(args) {
          setTimeout(function() {
            args.response.success({
              data: data
            });
          }, 50);
        };
        $listView = $('<div>').listView(listViewArgs);

        // Show list view of selected VMs
        $browser.cloudBrowser('addPanel', {
          title: 'Load Balanced VMs',
          data: '',
          noSelectPanel: true,
          maximizeIfSelected: true,
          complete: function($newPanel) {
            return $newPanel.listView(listViewArgs);
          }
        });
      },

      expandable: function(data) {
        var $expandable = $('<div>').addClass('expandable-listing');
        var $tbody = $('<tbody>').appendTo(
          $('<table>').appendTo($expandable)
        );

        $(data).each(function() {
          var field = this;
          var $tr = $('<tr>').appendTo($tbody);

          $tr.append(
            $('<td></td>').appendTo($tr).html(field.name)
          );
        });

        cloudStack.evenOdd($tbody, 'tr', {
          even: function($elem) {
            $elem.addClass('even');
          },
          odd: function($elem) {
            $elem.addClass('odd');
          }
        });

        return $expandable.hide();
      }
    }
  };

  $.fn.multiEdit = function(args) {
    var dataProvider = args.dataProvider;
    var multipleAdd = args.multipleAdd;
    var $multi = $('<div>').addClass('multi-edit').appendTo(this);
    var $multiForm = $('<form>').appendTo($multi);
    var $inputTable = $('<table>').addClass('multi-edit').appendTo($multiForm);
    var $dataTable = $('<div>').addClass('data').appendTo($multi);
    var $addVM;
    var fields = args.fields;
    var actions = args.actions;
    var noSelect = args.noSelect;
    var context = args.context;
    var ignoreEmptyFields = args.ignoreEmptyFields;
    var actionPreFilter = args.actionPreFilter;

    var $thead = $('<tr>').appendTo(
      $('<thead>').appendTo($inputTable)
    );
    var $inputForm = $('<tr>').appendTo(
      $('<tbody>').appendTo($inputTable)
    );
    var $dataBody = $('<div>').addClass('data-body').appendTo($dataTable);

    // Setup input table headers
    $.each(args.fields, function(fieldName, field) {
      var $th = $('<th>').addClass(fieldName).html(field.label.toString())
            .attr('rel', fieldName)
            .appendTo($thead);
      var $td = $('<td>').addClass(fieldName)
            .attr('rel', fieldName)
            .appendTo($inputForm);

      if (field.isHidden) {
        $th.hide();
        $td.hide();
      }

      if (field.select) {
        var $select = $('<select>')
              .attr({
                name: fieldName
              })
              .appendTo($td);

        field.select({
          $select: $select,
          $form: $multiForm,
          response: {
            success: function(args) {
              $(args.data).each(function() {
                $('<option>').val(this.name).html(this.description)
                  .appendTo($select);
              });

              _medit.refreshItemWidths($multi);
            }
          }
        });
      } else if (field.edit) {
        if (field.range) {
          var $range = $('<div>').addClass('range').appendTo($td);

          $(field.range).each(function() {
            $('<input>')
              .attr({
                name: this,
                type: 'text'
              })
              .addClass('required')
              .attr('disabled', field.isDisabled ? 'disabled' : false)
              .appendTo(
                $('<div>').addClass('range-item').appendTo($range)
              );
          });
        } else {
          $('<input>')
            .attr({
              name: fieldName,
              type: field.isPassword ? 'password' : 'text'
            })
            .addClass('required')
            .attr('disabled', field.isDisabled ? 'disabled' : false)
            .appendTo($td);
        }
      } else if (field.addButton) {
        $addVM = $('<div>').addClass('button add-vm').html(
          args.add.label
        ).appendTo($td);
      }
    });

    if (args.actions && !args.noHeaderActionsColumn) {
      $thead.append($('<th>Actions</th>').addClass('multi-actions'));
      $inputForm.append($('<td></td>').addClass('multi-actions'));
    }

    var vmList = function() {
      // Create a listing of instances, based on limited information
      // from main instances list view
      var $listView;
      var instances = $.extend(true, {}, args.listView, {
        uiCustom: true
      });

      instances.listView.actions = {
        select: {
          label: 'Select instance',
          type: multipleAdd ? 'checkbox' : 'radio',
          action: {
            uiCustom: function(args) {
              var $item = args.$item;
              var $input = $item.find('td.actions input:visible');

              if ($input.attr('type') == 'checkbox') {
                if ($input.is(':checked'))
                  $item.addClass('multi-edit-selected');
                else
                  $item.removeClass('multi-edit-selected');
              } else {
                $item.siblings().removeClass('multi-edit-selected');
                $item.addClass('multi-edit-selected');
              }
            }
          }
        }
      };

      $listView = $('<div>').listView(instances);

      // Change action label
      $listView.find('th.actions').html('Select');

      return $listView;
    };

    $addVM.bind('click', function() {
      // Validate form first
      if (!$multiForm.valid()) {
        if ($multiForm.find('input.error:visible').size()) {
          return false;
        }
      }

      var $dataList;
      var addItem = function(itemData) {
        var data = {};

        $.each(cloudStack.serializeForm($multiForm), function(key, value) {
          if (value != '') {
            data[key] = value;
          }
        });

        // Loading appearance
        var $loading = _medit.loadingItem($multi, 'Adding...');
        $dataBody.append($loading);

        // Clear out fields
        $multi.find('input').val('');

        // Apply action
        args.add.action({
          context: context,
          data: data,
          itemData: itemData,
          response: {
            success: function(successArgs) {
              var notification = successArgs.notification;
              if (notification) {
                $('.notifications').notifications('add', {
                  section: 'network',
                  desc: notification.label,
                  interval: 500,
                  _custom: successArgs._custom,
                  poll: function(pollArgs) {
                    var complete = pollArgs.complete;

                    notification.poll({
                      _custom: pollArgs._custom,
                      complete: function(completeArgs) {
                        complete(args);
                        $loading.remove();

                        _medit.addItem(
                          data,
                          args.fields,
                          $multi,
                          itemData,
                          args.actions,
                          {
                            multipleAdd: multipleAdd,
                            noSelect: noSelect,
                            context: context,
                            ignoreEmptyFields: ignoreEmptyFields,
                            preFilter: actionPreFilter
                          }
                        ).appendTo($dataBody);
                      }
                    });
                  }
                });
              }

              _medit.refreshItemWidths($multi);
            }
          }
        });
      };

      if (args.noSelect) {
        // Don't append instance data
        addItem([]);

        return true;
      }

      $dataList = vmList($multi).dialog({
        dialogClass: 'multi-edit-add-list panel',
        width: 825,
        title: args.add.label,
        buttons: [
          {
            text: 'Done',
            'class': 'ok',
            click: function() {
              $dataList.fadeOut(function() {
                addItem($.map(
                  $dataList.find('tr.multi-edit-selected'),

                  // Attach VM data to row
                  function(elem) {
                    return $(elem).data('json-obj');
                  }
                ));
                $dataList.remove();
              });

              $('div.overlay').fadeOut(function() {
                $('div.overlay').remove();
              });
            }
          },
          {
            text: 'Cancel',
            'class': 'cancel',
            click: function() {
              $dataList.fadeOut(function() {
                $dataList.remove();
              });
              $('div.overlay').fadeOut(function() {
                $('div.overlay').remove();
              });
            }
          }
        ]
      }).parent('.ui-dialog').overlay();

      return true;
    });

    // Get existing data
    dataProvider({
      context: context,
      response: {
        success: function(args) {
          $(args.data).each(function() {
            var data = this;
            var itemData = this._itemData;
            _medit.addItem(
              data,
              fields,
              $multi,
              itemData,
              actions,
              {
                multipleAdd: multipleAdd,
                noSelect: noSelect,
                context: $.extend(true, {}, context, this._context),
                ignoreEmptyFields: ignoreEmptyFields,
                preFilter: actionPreFilter
              }
            ).appendTo($dataBody);
          });

          _medit.refreshItemWidths($multi);
        }
      }
    });

    $multi.bind('change select', function() {
      _medit.refreshItemWidths($multi);
    });

    $multiForm.validate();

    return this;
  };

})(jQuery, cloudStack);
