(function($, cloudStack) {
  /**
   * Zone wizard
   */
  cloudStack.zoneWizard = function(args) {
    return function(listViewArgs) {
      var $wizard = $('#template').find('div.zone-wizard').clone();
      var $progress = $wizard.find('div.progress ul li');
      var $steps = $wizard.find('div.steps').children().hide().filter(':not(.disabled)');
      var $diagramParts = $wizard.find('div.diagram').children().hide();

      // Close wizard
      var close = function() {
        $wizard.dialog('destroy');
        $('div.overlay').fadeOut(function() { $('div.overlay').remove(); });
      };

      // Save and close wizard
      var completeAction = function() {
        var data = cloudStack.serializeForm($wizard.find('form'));
        args.action({
          data: data,
          response: {
            success: function(args) {
              var $item = $('.list-view').listView('prependItem', {
                data: [data],
                actionFilter: function(args) { return []; }
              });

              listViewArgs.complete({
                _custom: args._custom,
                $item: $item,
                messageArgs: {
                  name: $wizard.find('div.review div.vm-instance-name input').val()
                }
              });

              close();
            },
            error: function(message) {
              $wizard.remove();
              $('div.overlay').remove();

              if (message) {
                cloudStack.dialog.notice({ message: message });
              }
            }
          }
        });
      };

      // Go to specified step in wizard,
      // updating nav items and diagram
      var showStep = function(index) {
        var targetIndex = index - 1;

        if (index <= 1) targetIndex = 0;
        if (targetIndex == $steps.size()) {
          completeAction();
        }

        var $targetStep = $($steps.hide()[targetIndex]).show();
        var formState = cloudStack.serializeForm($wizard.find('form'));

        if (!targetIndex) {
          $wizard.find('.button.previous').hide();
        } else {
          $wizard.find('.button.previous').show();
        }

        // Hide conditional fields by default
        var $conditional = $targetStep.find('.conditional');
        $conditional.hide();

        // Show conditional fields for advanced network models
        if (formState['network-model'] == 'Advanced') {
          if (formState['isolation-mode'] == 'vlan') {
            $conditional.filter('.vlan').show().find('select').trigger('change');
            if ($conditional.find('select[name=vlan-type]').val() == 'tagged') {
              $conditional.find('select.ip-scope').trigger('change');
            }
          } else if (formState['isolation-mode'] == 'security-groups') {
            $conditional.filter('.security-groups').show();
          }
        }

        if (!formState['public']) {
          $conditional.filter('.public').show();
        }

        // Show launch button if last step
        var $nextButton = $wizard.find('.button.next');
        $nextButton.find('span').html('Next');
        $nextButton.removeClass('final');
        if ($targetStep.index() == $steps.size() - 1) {
          $nextButton.find('span').html('Add zone');
          $nextButton.addClass('final');
        }

        // Show relevant conditional sub-step if present
        if ($targetStep.has('.wizard-step-conditional')) {
          $targetStep.find('.wizard-step-conditional').hide();
          $targetStep.find('.wizard-step-conditional.select-network').show();
        }

        // Update progress bar
        var $targetProgress = $progress.removeClass('active').filter(function() {
          return $(this).index() <= targetIndex;
        }).toggleClass('active');

        // Load data provider for domain dropdowns
        if (!$targetStep.hasClass('loaded') && (index == 2 || index == 4)) {
          args.steps[targetIndex]({
            response: {
              success: function(args) {
                $(args.domains).each(function() {
                  $('<option>').val(this.id).html(this.name)
                    .appendTo($targetStep.find('select.domain'));
                });

                $targetStep.addClass('loaded');
              }
            }
          });
        }

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
          // Check validation first
          var $form = $steps.filter(':visible').find('form');
          if ($form.size() && !$form.valid()) {
            if ($form.find('.error:visible').size())
              return false;
          }

          showStep($steps.filter(':visible').index() + 2);

          return false;
        }

        // Previous button
        if ($target.closest('div.button.previous').size()) {
          showStep($steps.filter(':visible').index());

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

      showStep(1);

      // Setup tabs and slider
      $wizard.find('.tab-view').tabs();
      $wizard.find('.slider').slider({
        min: 1,
        max: 100,
        start: function(event) {
          $wizard.find('div.data-disk-offering div.custom-size input[type=radio]').click();
        },
        slide: function(event, ui) {
          $wizard.find('div.data-disk-offering div.custom-size input[type=text]').val(
            ui.value
          );
        }
      });

      return $wizard.dialog({
        title: 'Add zone',
        width: 665,
        height: 665,
        zIndex: 5000,
        resizable: false
      })
        .closest('.ui-dialog').overlay();
    };
  };
})(jQuery, cloudStack);
