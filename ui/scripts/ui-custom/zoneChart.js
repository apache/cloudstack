(function($, cloudStack) {
  /**
   * Make system zone 'org' chart
   */
  cloudStack.zoneChart = function(args) {
    return function(listViewArgs) {
      var $browser = listViewArgs.$browser;
      var $chartView = $('<div>').addClass('system-chart-view')
            .append(
              $('#template').find('div.zone-chart').clone()
            );
      args.dataProvider({
        id: listViewArgs.id,
        jsonObj: listViewArgs.jsonObj,
        context: { zones: listViewArgs.context.physicalResources },
        response: {
          success: function(dataProviderArgs) {
            var data = dataProviderArgs.data;
            var name = data.name;

            // Replace cell contents
            $chartView.find('li.zone div.name span').html(name);

            // Events
            $chartView.click(function(event) {
              var $target = $(event.target);
              var $panel;

              // View zone details button
              if ($target.is('ul li div.view-details')) {
                $panel = $browser.cloudBrowser('addPanel', {
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
              }

              // View all
              if ($target.is('ul li div.view-all')) {
                $panel = $browser.cloudBrowser('addPanel', {
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
              };

              // Add button
              if ($target.closest('.add').size()) {
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
                              poll: action.notification.poll
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
              }

              return true;
            });
          }
        }
      });

      return $chartView;
    };
  };
})(jQuery, cloudStack);
