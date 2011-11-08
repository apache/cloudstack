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

      // Renders individual network's chart
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

          var $item = $('<li>').addClass('provider')
                .addClass(name).addClass(status)
                .appendTo($networkProviders)
                .append($('<div>').addClass('name').html(type.label))
                .append($('<div>').addClass('status')
                        .append($('<span>').html(status)))
                .append($('<div>').addClass('view-all normal').html('View all'))
                .append($('<div>').addClass('view-all enable').html('Enable'));
        });

        // View all action
        $chartView.find('ul li div.view-all').click(function() {
          var $target = $(this);
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

        // Add button action
        $chartView.find('.add').click(function() {
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
              });

              $tabMain.tabs();
              $tabMain.find('li:first').addClass('first');
              $tabMain.find('li:last').addClass('last');
            }
          }
        });

        $refresh.click(function() {
          $charts.children().remove();
          loadNetworkData();

          return false;
        });
      };

      loadNetworkData();

      return $charts;
    };
  };
})(jQuery, cloudStack);
