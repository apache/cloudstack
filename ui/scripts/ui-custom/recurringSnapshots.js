(function(cloudStack, $) {
  cloudStack.uiCustom.recurringSnapshots = function(args) {
    var desc = args.desc;
    var selects = args.selects;
    var actions = args.actions;
    var dataProvider = args.dataProvider;

    return function(args) {
      var $snapshots = $('#template').find('.recurring-snapshots').clone();
      var context = args.context;

      // Get description
      $snapshots.find('.desc').html(desc);

      // Snapshot type tabs
      $snapshots.find('.forms').tabs();

      // Populate selects
      $snapshots.find('form select').each(function() {
        var $select = $(this);
        var selectData = selects[$select.attr('name')];

        if (selectData) {
          selectData({
            response: {
              success: function(args) {
                $(args.data).each(function() {
                  var $option = $('<option>').appendTo($select);

                  $option.val(this.id).html(this.name);
                });
              }
            }
          });
        }
      });

      // Form validation
      $snapshots.find('form').validate();

      // Add snapshot
      $snapshots.find('.add-snapshot-action.add').click(function() {
        var $form = $snapshots.find('form:visible');

        if (!$form.valid()) return false;

        var formData = cloudStack.serializeForm($form);

        actions.add({
          context: context,
          snapshot: formData,
          response: {
            success: function(args) {
              var $snapshotRow = $snapshots.find('.scheduled-snapshots tr').filter(function() {
                return $(this).index() == args.data.type;
              }).addClass('active').show();

              $snapshotRow.data('json-obj', args.data);

              // Update fields
              $snapshotRow.find('td.time span').html(args.data.time);
              $snapshotRow.find('td.day-of-week span').html(
                args.data['day-of-week'] ? 
                  $snapshots.find('select[name=day-of-week] option').filter(function() {
                    return $(this).val() == args.data['day-of-week'];
                  }).html() :
                  args.data['day-of-month']
              );
              $snapshotRow.find('td.timezone span').html(
                $snapshots.find('select[name=timezone] option').filter(function() {
                  return $(this).val() == args.data['timezone'];
                }).html()
              );
              $snapshotRow.find('td.keep span').html(args.data.keep);

              $(':ui-dialog').dialog('option', 'position', 'center');
              
              refreshSnapshotTabs();
            }
          }
        });

        return true;
      });

      // Enable/disable snapshot tabs based on table contents;
      var refreshSnapshotTabs = function() {
        $snapshots.find('li').each(function() {
          var index = $(this).index();
          var $tr = $snapshots.find('tr').filter(function() {
            return $(this).index() == index;
          });

          if ($tr.size() && $tr.hasClass('active')) { $(this).addClass('disabled ui-state-disabled'); }
          else { $(this).removeClass('disabled ui-state-disabled'); }

          if ($(this).is('.ui-tabs-selected.ui-state-disabled')) {
            $snapshots.find('form').show();

            if ($snapshots.find('li.ui-state-disabled').size() == $snapshots.find('li').size()) {
              $snapshots.find('form').hide();
            } else {
              $snapshots.find('li:not(.ui-state-disabled):first a').click();
            }
          }
        });
      };

      // Remove snapshot
      $snapshots.find('.action.destroy').click(function() {
        var $tr = $(this).closest('tr');
        actions.remove({
          context: context,
          snapshot: $tr.data('json-obj'),
          response: {
            success: function(args) {
              $tr.hide().removeClass('active');
              $(':ui-dialog').dialog('option', 'position', 'center');

              refreshSnapshotTabs();
            }
          }
        });
      });

      // Get existing data
      dataProvider({
        context: context,
        response: {
          success: function(args) {
            $(args.data).each(function() {
              var snapshot = this;

              // Get matching table row
              var $tr = $snapshots.find('tr').filter(function() {
                return $(this).index() == snapshot.type;
              }).addClass('active').show();

              $tr.data('json-obj', snapshot);

              $tr.find('td.time span').html(snapshot.time);
              $tr.find('td.timezone span').html(
                $snapshots.find('select[name=timezone] option').filter(function() {
                  return $(this).val() == snapshot['timezone'];
                }).html()
              );
              $tr.find('td.keep span').html(snapshot.keep);
              $tr.find('td.day-of-week span').html(
                snapshot['day-of-week'] ?
                  $snapshots.find('select[name=day-of-week] option').filter(function() {
                    return $(this).val() == snapshot['day-of-week'];
                  }).html() :
                  snapshot['day-of-month']
              );
            });

            refreshSnapshotTabs();
          }
        }
      });

      // Create dialog
      var $dialog = $snapshots.dialog({
        title: 'Recurring Snapshots',
        dialogClass: 'recurring-snapshots',
        width: 600,
        buttons: [
          {
            text: 'Done',
            'class': 'ok',
            click: function() {
              $dialog.fadeOut(function() {
                $dialog.remove();
              });

              $('div.overlay').fadeOut(function() {
                $('div.overlay').remove();
              });
            }
          }
        ]
      }).closest('.ui-dialog').overlay();

      return $dialog;
    };
  };
}(cloudStack, jQuery));