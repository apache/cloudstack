(function($, window) {
  window.cloudStack = {
    ui: {
      widgets: {} // Defines API methods for UI widgets
    },
    uiCustom: {}
  };

  /**
   * Generate navigation <li>s
   *
   * @param args cloudStack data args
   */
  var makeNavigation = function(args) {
    var $navList = $('<ul>');
    var preFilter = cloudStack.sectionPreFilter ?
          cloudStack.sectionPreFilter({
            context: $.extend(true, {}, args.context, {
              sections: $.map(cloudStack.sections, function(value, key) {
                return key;
              })
            })
          }) : null;

    $.each(args.sections, function(sectionID, args) {
      if (preFilter && $.inArray(sectionID, preFilter) == -1) {
        return true;
      }
      
      var $li = $('<li>')
            .addClass('navigation-item')
            .addClass(sectionID)
            .append($('<span>').addClass('icon').html('&nbsp;'))
            .append($('<span>').html(args.title))
            .data('cloudStack-section-id', sectionID);

      $li.appendTo($navList);

      return true;
    });

    // Special classes for first and last items
    $navList.find('li:first').addClass('first');
    $navList.find('li:last').addClass('last');

    return $navList;
  };

  /**
   * Create section contents
   *
   * @param sectionID Section's ID to show
   * @param args CloudStack3 configuration
   */
  var showSection = function(sectionID, args) {
    var $panel;
    var $browser = $('#browser div.container');
    var $navItem = $('#navigation').find('li').filter(function() {
      return $(this).hasClass(sectionID);
    });
    var data = args.sections[sectionID];
    data.$browser = $browser;

    $navItem.siblings().removeClass('active');
    $navItem.addClass('active');

    // Reset browser panels
    $browser.cloudBrowser('removeAllPanels');
    $panel = $browser.cloudBrowser('addPanel', {
      title: data.title,
      data: ''
    });

    // Hide breadcrumb if this is the home section
    if (args.home === sectionID) {
      $('#breadcrumbs').find('li:first, div.end:last').hide();
    }

    // Append specified widget to view
    if (data.show)
      $panel.append(data.show(data));
    else if (data.treeView)
      $panel.treeView(data, { context: args.context });
    else
      $panel.listView(data, { context: args.context });


    return $navItem;
  };

  // Define page element generation fns
  var pageElems = {
    header: function(args) {
      // Make notification area
      var $notificationArea = $('<div>').addClass('button notifications')
            .append(
              $('<div>').addClass('total')
                // Total notifications
                .append($('<span>').html(0))
            )
            .append($('<span>').html('Notifications'))
            .notifications();

      // Project switcher
      var $viewSwitcher = $('<div>').addClass('button view-switcher')
            .append(
              // Default View
              $('<div>').addClass('default-view')
                .html('Default View')
                .prepend(
                  $('<span>').addClass('icon').html('&nbsp;')
                )
            )
            .append(
              // Project View
              $('<div>').addClass('select')
                .html('Select View')
                .prepend(
                  $('<span>').addClass('icon').html('&nbsp;')
                )
            );

      // User status area
      var $userInfo = $('<div>').attr({ id: 'user' }).addClass('button')
            .append(
              $('<div>').addClass('name').html(
                args.context && args.context.users ?
                  (
                    args.context.users[0].name ?
                      args.context.users[0].name : args.context.users[0].login
                  ) : 'Invalid User'
              )
            )
            .append(
              $('<div>').addClass('icon options')
                .append(
                  $('<div>').addClass('icon arrow')
                )
            );

      return [
        $('<div>').addClass('logo'),
        $('<div>').addClass('controls')
          .append($notificationArea)
          .append($viewSwitcher)
          .append($userInfo)
      ];
    },

    'main-area': function(args) {
      var $navigation = $('<div>').attr({ id: 'navigation' });
      var $browser = $('<div>').attr({ id: 'browser' })
            .append(
              // Home breadcrumb
              $('<div>').attr({ id: 'breadcrumbs' })
                .append($('<div>').addClass('home'))
                .append($('<div>').addClass('end'))
            )

            .append(
              // Panel container
              $('<div>').addClass('container')
            );

      makeNavigation(args).appendTo($navigation);

      return [
        $navigation, $browser
      ];
    }
  };

  $.fn.cloudStack = function(args) {
    var $container = $('<div>')
          .attr({
            id: 'container',
            'cloudStack-container': true
          })
          .data('cloudStack-args', args)
          .appendTo(this);
    var context = args.context;

    // Create pageElems
    $.each(pageElems, function(id, fn) {
      var $elem = $('<div>').attr({ id: id });

      $(fn(args)).each(function() {
        $elem.append($(this));
      });

      $elem.appendTo($container);
    });

    // User options
    var $options = $('<div>').attr({ id: 'user-options' })
          .appendTo($('#header'));
    $(['Logout']).each(function() {
      $('<a>')
        .attr({ href: '#' })
        .html(this.toString())
        .appendTo($options);
    });

    // Initialize browser
    $('#browser div.container').cloudBrowser();
    $('#navigation li')
      .filter(function() {
        return $(this).hasClass(args.home);
      })
      .click();

    // Validation
    $.extend($.validator.messages, { required: 'Required field' });

    return this;
  };

  // Events
  $(function() {
    // Rollover behavior for user options
    $(document).bind('mouseover', function(event) {
      if ($(event.target).closest('#user, #user-options').size()) {
        return false;
      }
      else $('#user-options').hide();
    });

    $(document).bind('click', function(event) {
      var $target = $(event.target);
      var $container = $target.closest('[cloudStack-container]');
      var args = $container.data('cloudStack-args');

      if (!$container.size()) return true;

      // Navigation items
      if ($target.closest('li.navigation-item').size() && $target.closest('#navigation').size()) {
        var $navItem = $target.closest('li.navigation-item');
        showSection($navItem.data('cloudStack-section-id'), args);

        return false;
      }

      // Browser expand
      if ($target.hasClass('control expand') && $target.closest('div.panel div.toolbar').size()) {
        $('#browser div.container').cloudBrowser('toggleMaximizePanel', {
          panel: $target.closest('div.panel')
        });

        return false;
      }

      // Home breadcrumb
      if ($target.is('#breadcrumbs div.home')) {
        showSection(args.home, args);
        return false;
      }

      // Project buttons
      var $defaultSwitcher = $target.closest('div.controls div.button.view-switcher .default-view');
      if ($defaultSwitcher.size()) {
        $container.removeClass('project-view');
        $defaultSwitcher.closest('.view-switcher').removeClass('alt');
        $('#navigation li.dashboard').click();
        return false;
      }

      var $projectSwitcher = $target.closest('div.controls div.button.view-switcher .select');
      if ($projectSwitcher.size()) {
        $projectSwitcher.html('<span class="icon"></span>Projects');
        $projectSwitcher.closest('.view-switcher').addClass('alt');
        $('<div>')
          .addClass('sample-project-view')
          .appendTo($container)
          .overlay()
          .click(function(event) {
            $container.addClass('project-view');
            $('#navigation li.dashboard')
              .click();
            $('div.overlay').remove();
            $(this).remove();
            $('div.panel:first').children().remove();
            $('div.panel:first')
              .append(
                $('<img>')
                  .attr({ src: 'images/screens/ProjectDashboard.png' })
                  .css({ cursor: 'pointer' })
              );
            $('#breadcrumbs ul')
              .addClass('project-view')
              .prepend(
                $('<li>').addClass('active').append($('<span>').html('Project Name'))
              )
              .append(
                $('<div>').addClass('end')
              );
          });

        return false;
      }

      // User options
      if ($target.closest('#user div.icon.options').size()) {
        $('#user-options').toggle();

        return false;
      }

      return true;
    });
  });
})(jQuery, window);
