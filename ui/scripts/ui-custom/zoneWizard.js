(function($, cloudStack) {
  /**
   * Serialize form data as object
   */
  var getData = function($wizard, options) {
    if (!options) options = {};

    var $forms = $wizard.find('form').filter(function() {
      return !options.all ? !$(this).closest('.multi-edit').size() : true;
    });
    var $physicalNetworkItems = $wizard.find(
      '.steps .setup-physical-network .select-container.multi'
    ).filter(':not(.disabled)');
    var $publicTrafficItems = $wizard.find(
      '.steps .setup-public-traffic .data-body .data-item');
    var $storageTrafficItems = $wizard.find(
      '.steps .setup-storage-traffic .data-body .data-item');
    var groupedForms = {};

    if ($physicalNetworkItems.find('li.traffic-type-draggable.storage').size()) {
      $wizard.find('li.conditional.storage-traffic').show();
    } else {
      $wizard.find('li.conditional.storage-traffic').hide();
    }

    if (options.all) {
      return cloudStack.serializeForm($forms, { escapeSlashes: true });
    }

    // Group form fields together, by form ID
    $forms.each(function() {
      var $form = $(this);
      var id = $form.attr('rel');

      if (!id) return true;

      groupedForms[id] = cloudStack.serializeForm($form, { escapeSlashes: true });

      return true;
    });

    // Get physical network data
    groupedForms.physicalNetworks = $.map(
      $physicalNetworkItems,
      function(network) {
        var $network = $(network);
        var $guestForm = $wizard.find('form[guest-network-id=' + $network.index() + ']');

        return {
          id: $network.index(),
          name: $network.find('.field.name input[type=text]').val(),
          trafficTypes: $.map(
            $network.find('.traffic-type-draggable'),
            function(trafficType) {
              var $trafficType = $(trafficType);

              return $trafficType.attr('traffic-type-id');
            }
          ),
          guestConfiguration: $guestForm.size() ?
            cloudStack.serializeForm($guestForm) : null
        };
      }
    );

    // Get public traffic data (multi-edit)
    groupedForms.publicTraffic = $.map(
      $publicTrafficItems,
      function(publicTrafficItem) {
        var $publicTrafficItem = $(publicTrafficItem);
        var publicTrafficData = {};
        var fields = [
          'gateway',
          'netmask',
          'vlanid',
          'startip',
          'endip'
        ];

        $(fields).each(function() {
          publicTrafficData[this] =
            $publicTrafficItem.find('td.' + this + ' span').html();
        });

        return publicTrafficData;
      }
    );

    // Get storage traffic data (multi-edit)
    groupedForms.storageTraffic = $.map(
      $storageTrafficItems,
      function(storageTrafficItem) {
        var $storageTrafficItem = $(storageTrafficItem);
        var storageTrafficData = {};
        var fields = [
          'netmask',
          'vlanid',
          'startip',
          'endip'
        ];

        $(fields).each(function() {
          storageTrafficData[this] =
            $storageTrafficItem.find('td.' + this + ' span').html();
        });

        return storageTrafficData;
      }
    );

    // Hack to fix forward slash JS error
    $.each(groupedForms, function(key1, value1) {
      $.each(value1, function(key2, value2) {
        if (typeof value2 == 'string') {
          groupedForms[key1][key2] = value2.replace(/__forwardSlash__/g, '\/');
        }
      });
    });

    // Include zone network type
    if (groupedForms.zone) {
      groupedForms.zone.networkType = $forms.find('input[name=network-model]:checked').val();
    }

    return groupedForms;
  };

  /**
   * Handles validation for custom UI components
   */
  var customValidation = {
    networkRanges: function($form) {
      if ($form.closest('.multi-edit').find('.data-item').size()) {
        return true;
      }

      cloudStack.dialog.notice({
        message: dictionary['message.please.add.at.lease.one.traffic.range']
      });
      return false;
    }
  };

  /**
   * Determine if UI components in step should be custom-validated
   * (i.e., not a standard form)
   */
  var checkCustomValidation = function($step) {
    var $multiEditForm = $step.find('.multi-edit form');
    var $physicalNetworks = $step.find('.select-container.multi');
    var isCustomValidated;

    if ($multiEditForm.size()) {
      isCustomValidated = customValidation.networkRanges($multiEditForm);
    } else {
      isCustomValidated = true;
    }

    return isCustomValidated;
  };

  /**
   * Setup physical network wizard UI
   */
  var physicalNetwork = {
    init: function($wizard) {
      // Initialize initial physical networks
      for (var i = 0; i < 2; i++) {
        var $physicalNetwork = physicalNetwork.add($wizard);
        if (i == 0) {
          $physicalNetwork.find('.button.remove.physical-network').remove();
        }
      }

      // First physical network gets required traffic types
      $(physicalNetwork.requiredTrafficTypes).each(function () {
        var $firstPhysicalNetwork = physicalNetwork.getPhysicalNetworks($wizard).filter(':first');

        physicalNetwork.assignTrafficType(this, $firstPhysicalNetwork);
      });
    },

    /**
     * Required traffic type IDs for proper validation
     */
    requiredTrafficTypes: [
      'management',
      'public',
      'guest'
    ],

    /**
     * Physical network step: Renumber network form items
     */
    renumberFormItems: function($container) {
      var $items = $container.find('.select-container.multi');

      $items.each(function() {
        var $item = $(this);
        var $networkName = $item.find('.field.name input[type=text]');
        var $networkId = $item.find('input[name=id]');
        var $networkTypes = $item.find('.field.network-types input');
        var index = $item.index();

        $networkId.val(index);
        $networkName.attr('name', 'physicalNetworks[' + index + ']' + '.name');
        $networkTypes.val(index);
      });
    },

    /**
     * Get main physical network wizard step container
     *
     * @param $elem Any elem within the container
     */
    getMainContainer: function($elem) {
      return $elem.closest('.steps .setup-physical-network');
    },

    /**
     * Returns traffic elem
     *
     * @param trafficTypeID ID of desired traffic type
     */
    getTrafficType: function(trafficTypeID, $container) {
      var $trafficType = $container.find('li.traffic-type-draggable').filter(function() {
        return $(this).attr('traffic-type-id') == trafficTypeID;
      });

      if (physicalNetwork.isTrafficTypeClone($trafficType) && !$container.closest('.select-container.multi').size()) {
        // Get traffic type from original container
        return $trafficType.filter(function() {
          return $(this).closest(
            physicalNetwork.getOriginalTrafficContainer($trafficType)
          ).size();
        });
      }

      return $trafficType;
    },

    /**
     * Get original drag container for traffic type elem
     *
     * @param $trafficType Traffic type elem
     */
    getOriginalTrafficContainer: function($trafficType) {
      var $dragContainer = physicalNetwork.getMainContainer($trafficType)
        .find('.traffic-types-drag-area ul > li')
        .filter(function() {
          return $(this).hasClass($trafficType.attr('traffic-type-id'));
        })
        .find('ul');

      return $dragContainer;
    },

    /**
     * Get all physical networks
     *
     * @param $container Physical network step - main container
     */
    getPhysicalNetworks: function($container) {
      return $container.find('.select-container.multi');
    },

    /**
     * Determine if traffic type is a 'cloned' type
     *
     * @param $trafficType
     */
    isTrafficTypeClone: function($trafficType) {
      return $trafficType.hasClass('clone');
    },

    /**
     * Assigns traffic type to specified physical network
     *
     * @param trafficTypeID ID of desired traffic type
     * @param $physicalNetwork Physical network elem
     */
    assignTrafficType: function(trafficTypeID, $physicalNetwork) {
      var $container = physicalNetwork.getMainContainer($physicalNetwork);
      var $trafficType = physicalNetwork.getTrafficType(trafficTypeID, $container);
      var $dropArea = $physicalNetwork.find('.drop-container ul');

      if (physicalNetwork.isTrafficTypeClone($trafficType)) {
        if (!physicalNetwork.getTrafficType(trafficTypeID, $physicalNetwork).size()) {
          $trafficType = $trafficType.clone()
            .removeClass('disabled')
            .appendTo($dropArea)
            .draggable(physicalNetwork.draggableOptions($physicalNetwork.closest('.zone-wizard')));
        } else {
          return false;
        }
      } else {
        $trafficType.appendTo($dropArea);
      }

      physicalNetwork.update($.merge($physicalNetwork, $physicalNetwork.siblings()));

      return $trafficType;
    },

    /**
     * Assigns traffic type to original drag container
     *
     * @param trafficTypeID ID of desired traffic type
     * @param $container Physical network wizard step container
     * @param $physicalNetwork (optional) Specific physical network to remove from -- only for clones
     */
    unassignTrafficType: function(trafficTypeID, $container, $physicalNetwork) {
      var $trafficType = physicalNetwork.getTrafficType(trafficTypeID, $container);
      var $originalContainer = physicalNetwork.getOriginalTrafficContainer($trafficType);

      if (physicalNetwork.isTrafficTypeClone($trafficType)) {
        // Multiple traffic type elems present -- only get from physical network(s)
        if ($physicalNetwork) {
          $trafficType = physicalNetwork.getTrafficType(trafficTypeID, $physicalNetwork);
        } else {
          $trafficType = physicalNetwork.getTrafficType(trafficTypeID, physicalNetwork.getPhysicalNetworks($container));
        }

        $trafficType.remove();
      } else {
        // Just 1 traffic type elem present
        $trafficType.appendTo($originalContainer);
      }

      physicalNetwork.update(physicalNetwork.getPhysicalNetworks($container));

      return $trafficType;
    },

    /**
     * Returns true if new physical network item needs to be added
     */
    needNew: function($containers) {
      var $emptyContainers = $containers.filter(function() {
        return !$(this).find('li').size();
      });

      return !$emptyContainers.size() ? $containers.size() : false;
    },

    /**
     * Cleanup physical network containers
     */
    update: function($containers) {
      var $mainContainer = physicalNetwork.getMainContainer($containers);
      var $allPhysicalNetworks = physicalNetwork.getPhysicalNetworks($mainContainer);

      $allPhysicalNetworks.each(function() {
        var $ul = $(this).find('.drop-container ul');

        if (!$(this).find('li').size()) {
          $(this).addClass('disabled');
          $ul.fadeOut();
        } else {
          $(this).removeClass('disabled');
          $ul.show();
        }
      });

      $containers.each(function() {
        var $currentContainer = $(this);
        if (!$currentContainer.find('li').size() &&
            $containers.size() > 2) {
          $currentContainer.remove();
        }
      });

      $containers = $containers.closest('.setup-physical-network')
        .find('.select-container.multi');

      if (physicalNetwork.needNew($containers)) {
        physicalNetwork.add($mainContainer.parent());
      }

      return $containers;
    },

    /**
     * Default options for initializing traffic type draggables
     */
    draggableOptions: function($wizard) {
      return {
        appendTo: $wizard,
        helper: 'clone',

        // Events
        start: function(event, ui) {
          $(this).addClass('disabled');
        },

        stop: function(event, ui) {
          $(this).removeClass('disabled');
        }
      };
    },

    /**
     * Physical network step: Generate new network element
     */
    add: function($wizard) {
      var $container = $wizard.find('.setup-physical-network .content.input-area form');
      var $physicalNetworkItem = $('<div>').addClass('select-container multi');
      var $deleteButton = $('<div>').addClass('button remove physical-network')
        .attr({ title: 'Remove this physical network' })
        .append('<span>').addClass('icon').html('&nbsp;');
      var $icon = $('<div>').addClass('physical-network-icon');
      var $nameField = $('<div>').addClass('field name').append(
        $('<div>').addClass('name').append(
          $('<span>').html('Physical network name')
        ),
        $('<div>').addClass('value').append(
          $('<input>').attr({ type: 'text' }).addClass('required')
        )
      );
      var $dropContainer = $('<div>').addClass('drop-container').append(
        $('<span>').addClass('empty-message').html(
          'Drag and drop traffic types you would like to add here.'
        ),
        $('<ul>').hide()
      ).droppable({
        over: function(event, ui) {
          var $ul = $(this).find('ul');

          $ul.addClass('active');

          if (!$ul.find('li').size()) {
            $(this).closest('.select-container.multi').removeClass('disabled');
            $ul.fadeIn();
          }
        },

        out: function(event, ui) {
          var $ul = $(this).find('ul');

          $ul.removeClass('active');
          physicalNetwork.update($(this).closest('.select-container.multi'));
        },

        drop: function(event, ui) {
          var trafficTypeID = ui.draggable.attr('traffic-type-id');
          var $physicalNetwork = $(this).closest('.select-container.multi');

          if (trafficTypeID == 'guest' &&
            ui.draggable.closest('.select-container.multi').size()) {
            ui.draggable.remove();
          }

          physicalNetwork.assignTrafficType(trafficTypeID, $physicalNetwork);
        }
      });

      var $idField = $('<input>').attr({
        type: 'hidden',
        name: 'id',
        value: 0
      });

      // Initialize new default network form elem
      $physicalNetworkItem.append(
        $deleteButton,
        $icon,
        $nameField,
        $idField,
        $dropContainer
      );
      $physicalNetworkItem.hide().appendTo($container).fadeIn('fast');
      $physicalNetworkItem.find('.name input').val('Physical Network ' + parseInt($physicalNetworkItem.index() + 1));
      physicalNetwork.renumberFormItems($container);

      // Remove network action
      $physicalNetworkItem.find('.button.remove.physical-network').click(function() {
        $physicalNetworkItem.find('li.traffic-type-draggable').each(function() {
          var trafficTypeID = $(this).attr('traffic-type-id');

          physicalNetwork.assignTrafficType(trafficTypeID, $physicalNetworkItem.prev());
        });

        $physicalNetworkItem.find('li.traffic-type-draggable.clone').remove();
        physicalNetwork.update($physicalNetworkItem.parent().find('.multi'));
      });

      $physicalNetworkItem.addClass('disabled'); // Since there are no traffic types yet

      return $physicalNetworkItem;
    },

    /**
     * Physical network step: Remove specified network element
     */
    remove: function($item) {
      var $container = $item.closest('.setup-physical-network .content.input-area form');

      if (!$item.siblings().size()) {
        cloudStack.dialog.notice({
          message: dictionary['message.you.must.have.at.least.one.physical.network']
        });
      } else if ($item.find('input[type=radio]:checked').size()) {
        cloudStack.dialog.notice({
          message: dictionary['message.please.select.a.different.public.and.management.network.before.removing']
        });
      } else {
        // Put any traffic type symbols back in original container
        $item.find('li.traffic-type-draggable').each(function() {
          var $draggable = $(this);
          var $originalContainer = $('.traffic-types-drag-area:visible > ul > li')
            .filter(function() {
              return $(this).hasClass($draggable.attr('traffic-type-id'));
            });

          $draggable.appendTo($item.prev());
        });

        $item.remove();
      }

      $container.validate('refresh');
    }
  };

  /**
   * Configure guest traffic UI
   */
  var guestTraffic = {
    init: function($wizard, args) {
      var $physicalNetworks = physicalNetwork.getPhysicalNetworks($wizard);
      var $tabs = guestTraffic.makeTabs($physicalNetworks, args);
      var $container = guestTraffic.getMainContainer($wizard);

      // Cleanup
      guestTraffic.remove($wizard);

      $container.find('.content form').hide();
      $tabs.appendTo($container.find('.content .select-container'));
      var $subnav = $container.find('ul.subnav').remove(); // Fix to avoid subnav becoming tab ul
      $container.tabs();
      $container.prepend($subnav);
      $container.find('.field').hide();
      $container.find('[rel=vlanRange]').show();
    },

    /**
     * Cleanup
     */
    remove: function($wizard) {
      var $container = guestTraffic.getMainContainer($wizard);

      // Cleanup
      $container.tabs('destroy');
      $container.find('.physical-network-item').remove();
      $container.find('.content form').show();
    },

    getMainContainer: function($wizard) {
      return $wizard.find('.steps .setup-guest-traffic');
    },

    makeTabs: function($physicalNetworks, args) {
      var $tabs = $('<ul></ul>').addClass('physical-network-item');
      var $content = $();

      // Only use networks w/ guest traffic type
      $physicalNetworks = $physicalNetworks.filter(function() {
        return $(this).find('li.guest').size();

        return true;
      });

      $physicalNetworks.each(function() {
        var $network = $(this);
        var $form = makeForm(args, 'guestTraffic', {});
        var refID = $network.find('input[name=id]').val();
        var networkID = 'physical-network-' + refID;

        $form.attr('guest-network-id', refID);

        $tabs.append($('<li></li>').append(
          $('<a></a>')
            .attr({ href: '#' + networkID })
            .html($network.find('.field.name input').val())
        ));
        $.merge(
          $content,
          $('<div></div>')
            .addClass('physical-network-item')
            .attr({ id: networkID })
            .append($form)
        );

        $form.validate();
      });

      $tabs.find('li:first').addClass('first');
      $tabs.find('li:last').addClass('last');

      return $.merge($tabs, $content);
    }
  };

  /**
   * Generate dynamic form, based on ID of form object given
   */
  var makeForm = function(args, id, formState) {
    var form = cloudStack.dialog.createForm({
      noDialog: true,
      context: $.extend(true, {}, cloudStack.context, {
        zones: [formState]
      }),
      form: {
        title: '',
        desc: '',
        fields: args.forms[id].fields
      },
      after: function(args) {}
    });

    var $form = form.$formContainer.find('form');

    // Cleanup form to follow zone wizard CSS naming
    $form.attr('rel', id);
    $form.find('input[type=submit]').remove();
    $form.find('.form-item').addClass('field').removeClass('form-item');
    $form.find('label.error').hide();
    $form.find('.form-item .name').each(function() {
      $(this).html($(this).find('label'));
    });

    $form.find('select, input').change(function() {
      cloudStack.evenOdd($form, '.field:visible', {
        even: function($row) {
          $row.removeClass('odd');
        },
        odd: function($row) {
          $row.addClass('odd');
        }
      });
    });

    return $form;
  };

  cloudStack.uiCustom.zoneWizard = function(args) {
    return function() {
      var $wizard = $('#template').find('div.zone-wizard').clone();
      var $progress = $wizard.find('div.progress ul li');
      var $steps = $wizard.find('div.steps').children().hide().filter(':not(.disabled)');

      $wizard.data('startfn', null);

      // Close wizard
      var close = function() {
        $wizard.dialog('destroy');
        $('div.overlay').fadeOut(function() { $('div.overlay').remove(); });
      };

      // Save and close wizard
      var completeAction = function() {
        var $launchStep = $wizard.find('.steps .review');
        var data = getData($wizard);
        var enableZoneAction = args.enableZoneAction;

        // Convert last step to launch appearance
        $launchStep.find('.main-desc').hide();
        $launchStep.find('.main-desc.launch').show();
        $launchStep.find('.launch-container').show();
        $launchStep.find('ul').html('');
        $wizard.find('.buttons').hide();
        $wizard.find('.button.previous').remove();

        var makeMessage = function(message, isError) {
          var $li = $('<li>')
            .addClass(!isError ? 'loading' : 'info')
            .append(
              $('<span>').addClass('icon').html('&nbsp;'),
              $('<span>').addClass('text').html(message)
            );
          var $launchContainer = $launchStep.find('.launch-container');

          $launchStep.find('ul').append($li);
          $li.prev().removeClass('loading');
          $launchContainer.scrollTop($launchContainer.height());

          if (isError) {
            $li.prev().addClass('error');
          }
          
        };

        args.action({
          data: data,
          startFn: $wizard.data('startfn'),
          uiSteps: $.map(
            $wizard.find('.steps > div'),
            function(step) {
              return $(step).attr('zone-wizard-step-id');
            }
          ),
          response: {
            success: function(args) {
              $launchStep.find('ul li').removeClass('loading');

              var closeWindow = function() {
                close();
                $(window).trigger('cloudStack.fullRefresh');
              };

              var enableZone = function() {
                makeMessage(dictionary['message.enabling.zone']);

                enableZoneAction({
                  formData: data,
                  data: data,
                  launchData: args.data,
                  response: {
                    success: function(args) {
                      closeWindow();
                    },

                    error: function(message) {
                      cloudStack.dialog.notice({ message: dictionary['error.could.not.enable.zone'] + ':</br>' + message });
                    }
                  }
                });
              };

              cloudStack.dialog.confirm({
                message: dictionary['message.zone.creation.complete.would.you.like.to.enable.this.zone'],
                action: function() {
                  enableZone();
                },
                cancelAction: function() {
                  closeWindow();
                }
              });
            },
            error: function(stepID, message, start) {
              var goNextOverride = function(event) {
                $(this).unbind('click', goNextOverride);
                showStep(stepID, false, { nextStep: 'launch' });

                return false;
              };

              $wizard.find('.buttons').show();
              $wizard.find('.buttons .button.next')
                .removeClass('final')
                .html('<span>Fix errors</span>')
                .click(goNextOverride);
              makeMessage(dictionary['error.something.went.wrong.please.correct.the.following'] + ':<br/>' + message, true);
              $wizard.data('startfn', start);
            },
            message: makeMessage
          }
        });
      };

      // Go to specified step in wizard,
      // updating nav items and diagram
      var showStep = function(index, goBack, options) {
        if (!options) options = {};

        if (typeof index == 'string') {
          index = $wizard.find('[zone-wizard-step-id=' + index + ']').index() + 1;
        }

        var targetIndex = index - 1;

        if (index <= 1) targetIndex = 0;
        if (targetIndex == $steps.size()) {
          completeAction();
        }

        $steps.hide();
        $wizard.find('.buttons').show();

        var $targetStep = $($steps[targetIndex]).show();
        var $uiCustom = $targetStep.find('[ui-custom]');
        var formState = getData($wizard, { all: true });
        var groupedFormState = getData($wizard);
        var formID = $targetStep.attr('zone-wizard-form');
        var stepPreFilter = cloudStack.zoneWizard.preFilters[
          $targetStep.attr('zone-wizard-prefilter')
        ];

        // Bypass step check
        if (stepPreFilter && !stepPreFilter({ data: formState, groupedData: groupedFormState })) {
          return showStep(
            !goBack ? index + 1 : index - 1,
            goBack
          );
        }

        if (formID) {
          if (!$targetStep.find('form').size()) {
            makeForm(args, formID, formState).appendTo($targetStep.find('.content.input-area .select-container'));

            setTimeout(function() {
              cloudStack.evenOdd($targetStep, '.field:visible', {
                even: function() {},
                odd: function($row) {
                  $row.addClass('odd');
                }
              });
            }, 50);
          } else {
            // Always re-activate selects
            $targetStep.find('form select').each(function() {
              var $select = $(this);
              var selectFn = $select.data('dialog-select-fn');
              var originalVal = $select.val();

              if (selectFn) {
                $select.html('');
                selectFn({
                  context: {
                    zones: [formState]
                  }
                });
                $select.val(originalVal);
                $select.trigger('change');
              }
            });
          }

          if (args.forms[formID].preFilter) {
            var preFilter = args.forms[formID].preFilter({
              $form: $targetStep.find('form'),
              data: formState
            });
          }
        }

        // Custom UI manipulations for specific steps
        switch($targetStep.attr('zone-wizard-step-id')) {
          case 'configureGuestTraffic':
            if (formState['network-model'] == 'Advanced') {
              guestTraffic.init($wizard, args);
            } else {
              guestTraffic.remove($wizard);
            }
            break;
        }

        if ($uiCustom.size()) {
          $uiCustom.each(function() {
            var $item = $(this);
            var id = $item.attr('ui-custom');

            $item.replaceWith(
              args.customUI[id]({
                data: formState,
                context: cloudStack.context
              })
            )
          });
        }

        if (!targetIndex) {
          $wizard.find('.button.previous').hide();
        } else {
          $wizard.find('.button.previous').show();
        }

        var $nextButton = $wizard.find('.button.next');
        $nextButton.find('span').html('Next');
        $nextButton.removeClass('final post-launch');

        // Show launch button if last step
        if ($targetStep.index() == $steps.size() - 1 || options.nextStep) {
          $nextButton.find('span').html(options.nextStep ? 'Save changes' : 'Launch zone');
          $nextButton.addClass('final');

          if (options.nextStep) { $nextButton.addClass('post-launch'); }
        }

        // Update progress bar
        var $targetProgress = $progress.removeClass('active').filter(function() {
          return $(this).index() <= targetIndex;
        }).toggleClass('active');

        setTimeout(function() {
          if (!$targetStep.find('input[type=radio]:checked').size()) {
            $targetStep.find('input[type=radio]:first').click();
          }
        }, 50);

        $targetStep.find('form').validate();
      };

      // Events
      $wizard.find('select').change(function(event) {
        // Conditional selects (on step 4 mainly)
        var $target = $(this);
        var $tagged = $wizard.find('.conditional.vlan-type-tagged');
        var $untagged = $wizard.find('.conditional.vlan-type-untagged');
        var $accountSpecific = $wizard.find('.field.conditional.ip-scope-account-specific');

        // VLAN - tagged
        if ($target.is('[name=vlan-type]')) {
          $tagged.hide();
          $untagged.hide();
          $accountSpecific.hide();

          if ($target.val() == 'tagged') {
            $untagged.hide();
            $tagged.show();
          }
          else if ($target.val() == 'untagged') {
            $tagged.hide();
            $untagged.show();
          }

          $.merge($tagged, $untagged).find('select:visible').trigger('change');

          cloudStack.evenOdd($wizard, '.field:visible', {
            even: function($elem) { $elem.removeClass('odd'); $elem.addClass('even'); },
            odd: function($elem) { $elem.removeClass('even'); $elem.addClass('odd'); }
          });

          return true;
        }

        // IP Scope - acct. specific
        if ($target.is('select.ip-scope')) {
          $accountSpecific.hide();
          if ($target.val() == 'account-specific') $accountSpecific.show();

          cloudStack.evenOdd($wizard, '.field:visible', {
            even: function($elem) { $elem.removeClass('odd'); $elem.addClass('even'); },
            odd: function($elem) { $elem.removeClass('even'); $elem.addClass('odd'); }
          });
        }

        return true;
      });

      $wizard.click(function(event) {
        var $target = $(event.target);

        // Radio button
        if ($target.is('[type=radio]')) {

          if ($target.attr('name') == 'network-model') {
            var $inputs = $wizard.find('.isolation-mode').find('input[name=isolation-mode]').attr({
              disabled: 'disabled'
            });

            if ($target.val() == 'Advanced') {
              $inputs.attr('disabled', false);
            }
          }

          return true;
        }

        // Checkbox
        if ($target.is('[type=checkbox]:checked')) {
          $('div.conditional.' + $target.attr('name')).hide();

          return true;
        } else if ($target.is('[type=checkbox]:unchecked')) {
          $('div.conditional.' + $target.attr('name')).show();

          return true;
        }

        // Next button
        if ($target.closest('div.button.next').size()) {
          var $step = $steps.filter(':visible');
          // Validation
          var $form = $('form:visible').filter(function() {
            // Don't include multi-edit (validation happens separately)
            return !$(this).closest('.multi-edit').size();
          });

          // Handle validation for custom UI components
          var isCustomValidated = checkCustomValidation($step);
          if (($form.size() && !$form.valid()) || !isCustomValidated) {
            if (($form && $form.find('.error:visible').size()) || !isCustomValidated)
              return false;
          }

          if (!$target.closest('.button.next.final').size())
            showStep($steps.filter(':visible').index() + 2);
          else {
            if ($target.closest('.button.next.final.post-launch').size()) {
              showStep('launch');
            }

            completeAction();
          }

          return false;
        }

        // Previous button
        if ($target.closest('div.button.previous').size()) {
          showStep($steps.filter(':visible').index(), true);

          return false;
        }

        // Close button
        if ($target.closest('div.button.cancel').size()) {
          close();

          return false;
        }

        // Edit link
        if ($target.closest('div.edit').size()) {
          var $edit = $target.closest('div.edit');

          showStep($edit.find('a').attr('href'));

          return false;
        }

        return true;
      });

      // Add/remove network action
      $wizard.find('.button.add.new-physical-network').click(function() {
        addPhysicalNetwork($wizard);
      });

      // Traffic type draggables
      $wizard.find('.traffic-type-draggable').draggable(physicalNetwork.draggableOptions($wizard));

      // For removing traffic types from physical network
      $wizard.find('.traffic-types-drag-area').droppable({
        drop: function(event, ui) {
          var trafficTypeID = ui.draggable.attr('traffic-type-id');

          if (!physicalNetwork.isTrafficTypeClone(ui.draggable)) {
            if ($.inArray(trafficTypeID, physicalNetwork.requiredTrafficTypes) == -1) {
              physicalNetwork.unassignTrafficType(trafficTypeID, $wizard);
            } else {
              physicalNetwork.assignTrafficType(
                trafficTypeID,
                $wizard.find('.select-container.multi:first')
              );
            }
          } else if (!ui.draggable.closest('.traffic-types-drag-area').size()) {
            ui.draggable.remove();
          }

          return true;
        }
      });

      physicalNetwork.init($wizard);

      showStep(1);

      return $wizard.dialog({
        title: 'Add zone',
        width: 750,
        height: 665,
        zIndex: 5000,
        resizable: false
      }).closest('.ui-dialog').overlay();
    };
  };
})(jQuery, cloudStack);
