(function($, cloudStack) {
  /**
   * Instance wizard
   */
  cloudStack.instanceWizard = function(args) {
    return function(listViewArgs) {
      var instanceWizard = function(data) {
        var $wizard = $('#template').find('div.instance-wizard').clone();
        var $progress = $wizard.find('div.progress ul li');
        var $steps = $wizard.find('div.steps').children().hide();
        var $diagramParts = $wizard.find('div.diagram').children().hide();
        var $form = $wizard.find('form');

        $form.validate();

        // Close instance wizard
        var close = function() {
          $wizard.dialog('destroy');
          $('div.overlay').fadeOut(function() { $('div.overlay').remove(); });
        };

        // Save instance and close wizard
        var completeAction = function() {
          var data = cloudStack.serializeForm($form);
          
          args.action({
            // Populate data
            data: data,
            response: {
              success: function(args) {
                var $loading = $('.list-view').listView('prependItem', {
                  data: [
                    {
                      name: data.displayname ? data.displayname : 'New VM',
                      zonename: $wizard.find('select[name=zoneid] option').filter(function() {
                        return $(this).val() == data.zoneid;
                      }).html(),
                      state: 'Creating'
                    }
                  ],
                  actionFilter: function(args) { return []; }
                });

                listViewArgs.complete({
                  _custom: args._custom,
                  messageArgs: cloudStack.serializeForm($form),
                  $item: $loading
                });

                close();
              },
              error: function(args) {
                $wizard.remove();
                $('div.overlay').remove();

                if (args.message) {
                  //cloudStack.dialog.notice({ message: args.message });
                }
              }
            }
          });
        };

        var makeSelects = function(name, data, fields, options) {
          var $selects = $('<div>');
          options = options ? options : {};

          $(data).each(function() {
            var id = this[fields.id];
            
            var $select = $('<div>')
                  .addClass('select')
                  .append(
                    $('<input>')
                      .attr({
                        type: (function(type) {
                          return type ? type : 'radio';
                        })(options ? options.type : null),
                        name: name,
                        'wizard-field': options['wizard-field']
                      })
                      .val(id)
                      .click(function() {
                        var $radio = $(this).closest('.select').find('input[type=radio]');

                        if ($radio.is(':checked') && !$(this).is(':checked')) {
                          return false; 
                        }

                        return true;
                      })
                  )
                  .append(
                    $('<div>').addClass('select-desc')
                      .append($('<div>').addClass('name').html(this[fields.name]))
                      .append($('<div>').addClass('desc').html(this[fields.desc]))
                  );

            $selects.append($select);

            if (options.secondary) {
              var $secondary = $('<div>').addClass('secondary-input').append(
                $('<input>')
                  .attr({
                    type: options.secondary.type,
                    name: options.secondary.name
                  })
                  .val(id)
                  .click(function() {
                    var $checkbox = $(this).closest('.select').find('input[type=checkbox]');

                    if (!$checkbox.is(':checked')) {
                      $checkbox.attr('checked', true);
                    }
                  })
                  .after(
                    $('<div>').addClass('name').html(options.secondary.desc)
                  )
              ).appendTo($select);
            }
          });

          cloudStack.evenOdd($selects, 'div.select', {
            even: function($elem) {
              $elem.addClass('even');
            },
            odd: function($elem) {
              $elem.addClass('odd');
            }
          });

          return $selects.children();
        };

        var dataProvider = function(step, providerArgs, callback) {
          // Call appropriate data provider
          args.steps[step - 1]($.extend(providerArgs, {
            currentData: cloudStack.serializeForm($form)
          }));
        };

        var dataGenerators = {
          setup: function($step, formData) {
            var originalValues = function(formData) {
              $step.find('select').val(
                formData.zoneid
              );

              $step.find('input[type=radio]').filter(function() {
                return $(this).val() == formData['select-template'];
              }).click();
            };

            return {
              response: {
                success: function(args) {
                  // Zones
                  $(args.data.zones).each(function() {
                    $step.find('.select-zone select').append(
                      $('<option>')
                        .attr({
                          value: this.id,
                          'wizard-field': 'zone'
                        })
                        .html(this.name)
                    );
                  });

                  originalValues(formData);
                }
              }
            };
          },

          'select-iso': function($step, formData) {
            var originalValues = function(formData) {
              var $inputs = $step.find('.wizard-step-conditional:visible')
                    .find('input[type=radio]');
              var $selected = $inputs.filter(function() {
                return $(this).val() == formData.templateid;
              });

              if (!$selected.size()) {
                $inputs.filter(':first').click();
              } else {
                $selected.click();
              }
              $step.find('select[name=hypervisorid]:visible').val(
                formData.hypervisorid
              );
            };

            $step.find('.wizard-step-conditional').hide();

            return {
              response: {
                success: function(args) {
                  if (formData['select-template']) {
                    $step.find('.wizard-step-conditional').filter(function() {
                      return $(this).hasClass(formData['select-template']);
                    }).show();
                  } else {
                    $step.find('.select-iso').show();
                  }
                  var makeIsos = function(type, append) {
                    var $selects = makeSelects('templateid', args.data.templates[type], {
                      name: 'name',
                      desc: 'displaytext',
                      id: 'id'
                    }, {
                      'wizard-field': 'template'
                    });

                    if (type == 'isos') {
                      // Create hypervisor select
                      $selects.find('input').bind('click', function() {
                        var $select = $(this).closest('.select');
                        $select.siblings().removeClass('selected').find('.hypervisor').remove();
                        $select.addClass('selected').append(
                          $('<div>').addClass('hypervisor')
                            .append($('<label>').html('Hypervisor:'))
                            .append($('<select>').attr({ name: 'hypervisorid' }))
                        );

                        // Get hypervisor data
                        $(args.data.hypervisors).each(function() {
                          $select.find('select').append(
                            $('<option>').attr({
                              value: this[args.hypervisor.idField],
                              'wizard-field': 'hypervisor'
                            })
                              .html(this[args.hypervisor.nameField])
                          );
                        });
                      });
                    }

                    append($selects);
                  };

                  // Featured ISOs
                  $(
                    [
                      ['featuredtemplates', 'instance-wizard-featured-templates'],
                      ['communitytemplates', 'instance-wizard-community-templates'],
					            ['mytemplates', 'instance-wizard-my-templates'],
                      ['isos', 'instance-wizard-my-isos']
                    ]
                  ).each(function() {
                    var item = this;
                    var $selectContainer = $wizard.find('#' + item[1]).find('.select-container');

                    makeIsos(item[0], function($elem) {
                      $selectContainer.append($elem);
                    });
                  });

                  originalValues(formData);
                }
              }
            };
          },

          'service-offering': function($step, formData) {
            var originalValues = function(formData) {
              if (formData.serviceofferingid) {
                $step.find('input[type=radio]').filter(function() {
                  return $(this).val() == formData.serviceofferingid;
                }).click(); 
              } else {
                $step.find('input[type=radio]:first').click();
              }
            };

            return {
              response: {
                success: function(args) {
                  $step.find('.content .select-container').append(
                    makeSelects('serviceofferingid', args.data.serviceOfferings, {
                      name: 'name',
                      desc: 'displaytext',
                      id: 'id'
                    }, {
                      'wizard-field': 'service-offering'
                    })
                  );

                  originalValues(formData);
                }
              }
            };
          },

          'data-disk-offering': function($step, formData) {
            var originalValues = function(formData) {
              var $targetInput = $step.find('input[type=radio]').filter(function() {
                return $(this).val() == formData.diskofferingid;
              }).click();

              if (!$targetInput.size()) {
                $step.find('input[type=radio]:visible').filter(':first').click();
              }
            };

            $step.find('.section.custom-size').hide();

            return {
              response: {
                success: function(args) {
                  $step.removeClass('custom-disk-size');
                  if (args.required) {
                    $step.find('.section.no-thanks').hide();
                    $step.addClass('required');
                  } else {
                    $step.find('.section.no-thanks').show();
                    $step.removeClass('required');
                  }

                  $step.find('.content .select-container').append(
                    makeSelects('diskofferingid', args.data.diskOfferings, {
                      id: 'id',
                      name: 'name',
                      desc: 'displaytext'
                    }, {
                      'wizard-field': 'disk-offering'
                    })
                  );

                  $step.find('input[type=radio]').bind('change', function() {
                    var $target = $(this);
                    var val = $target.val();
                    var item = $.grep(args.data.diskOfferings, function(elem) {
                      return elem.id == val;
                    })[0];
                    
                    if (!item) return true;

                    var custom = item[args.customFlag];

                    $step.find('.custom-size-label').remove();

                    if (custom) {
                      $target.parent().find('.name')
                        .append(
                          $('<span>').addClass('custom-size-label')
                            .append(': ')
                            .append(
                              $('<span>').addClass('custom-disk-size').html(
                                $step.find('.custom-size input[name=size]').val()
                              )
                            )
                            .append(' GB')
                        );
                      $target.parent().find('.select-desc .desc')
                        .append(
                          $('<span>').addClass('custom-size-label')
                            .append(', ')
                            .append(
                              $('<span>').addClass('custom-disk-size').html(
                                $step.find('.custom-size input[name=size]').val()
                              )
                            )
                            .append(' GB')
                        );
                      $step.find('.section.custom-size').show();
                      $step.addClass('custom-disk-size');
                      $target.closest('.select-container').scrollTop(
                        $target.position().top
                      );
                    } else {
                      $step.find('.section.custom-size').hide();
                      $step.removeClass('custom-disk-size');
                    }

                    return true;
                  });

                  originalValues(formData);
                }
              }
            };
          },

          'network': function($step, formData) {
            var originalValues = function(formData) {
              // Default networks
              $step.find('input[name=my-networks]').filter(function() {
                return $(this).val() == formData['my-networks'];
              }).click();

              // Optional networks
              var selectedOptionalNetworks = [];

              if ($.isArray(formData['shared-networks'])) {
                $(formData['shared-networks']).each(function() {
                  selectedOptionalNetworks.push(this);
                });
              } else {
                selectedOptionalNetworks.push(formData['shared-networks']);
              }

              var $checkboxes = $step.find('input[name=shared-networks]');
              $(selectedOptionalNetworks).each(function() {
                var networkID = this;
                $checkboxes.filter(function() {
                  return $(this).val() == networkID;
                }).attr('checked', 'checked');
              });
            };

            // Show relevant conditional sub-step if present
            $step.find('.wizard-step-conditional').hide();

            return {
              response: {
                success: function(args) {
                  if (args.type) {
                    $step.find('.wizard-step-conditional').filter(function() {
                      return $(this).hasClass(args.type);
                    }).show();
                  } else {
                    $step.find('.select-network').show();
                  }

                  // My networks
                  $step.find('.my-networks .select-container').append(
                    makeSelects('my-networks', args.data.myNetworks, {
                      name: 'name',
                      desc: 'displaytext',
                      id: 'id'
                    }, {
                      type: 'checkbox',
                      'wizard-field': 'my-networks',
                      secondary: {
                        desc: 'Default',
                        name: 'defaultNetwork',
                        type: 'radio'
                      }
                    })
                  );

                  // Shared networks
                  $step.find('.shared-networks .select-container').append(
                    makeSelects('shared-networks', args.data.sharedNetworks, {
                      name: 'name',
                      desc: 'displaytext',
                      id: 'id'
                    }, {
                      type: 'checkbox',
                      secondary: {
                        desc: 'Default',
                        name: 'defaultNetwork',
                        type: 'radio'
                      }
                    })
                  );

                  // Security groups (alt. page)
                  $step.find('.security-groups .select-container').append(
                    makeSelects('security-groups', args.data.securityGroups, {
                      name: 'name',
                      desc: 'description',
                      id: 'id'
                    }, {
                      type: 'checkbox'
                    })
                  );

                  originalValues(formData);
                }
              }
            };
          },

          'review': function($step, formData) {
            return {
              response: {
                success: function(args) {
                  $step.find('[wizard-field]').each(function() {
                    var field = $(this).attr('wizard-field');
                    var fieldName;
                    var $input = $wizard.find('[wizard-field=' + field + ']').filter(function() {
                      return $(this).is(':selected') || $(this).is(':checked');
                    });

                    if ($input.is('option')) {
                      fieldName = $input.html();
                    } else if ($input.is('input[type=radio]')) {
                      fieldName = $input.parent().find('.select-desc .name').html();
                    }

                    if (fieldName) {
                      $(this).html(fieldName);                      
                    } else {
                      $(this).html('(None)');
                    }
                  });
                }
              }
            };
          }
        };

        // Go to specified step in wizard,
        // updating nav items and diagram
        var showStep = function(index) {
          var targetIndex = index - 1;

          if (index <= 1) targetIndex = 0;
          if (targetIndex == $steps.size()) {
            completeAction();
            return;
          }

          var $targetStep = $($steps.hide()[targetIndex]).show();
          var stepID = $targetStep.attr('wizard-step-id');
          var formData = cloudStack.serializeForm($form);

          if (!$targetStep.hasClass('review')) { // Review row content is not autogenerated
            $targetStep.find('.select-container div, option').remove();
          }

          dataProvider(
            index,
            dataGenerators[stepID](
              $targetStep,
              formData
            )
          );

          // Show launch vm button if last step
          var $nextButton = $wizard.find('.button.next');
          $nextButton.find('span').html('Next');
          $nextButton.removeClass('final');
          if ($targetStep.hasClass('review')) {
            $nextButton.find('span').html('Launch VM');
            $nextButton.addClass('final');
          }

          // Update progress bar
          var $targetProgress = $progress.removeClass('active').filter(function() {
            return $(this).index() <= targetIndex;
          }).toggleClass('active');

          // Update diagram; show/hide as necessary
          $diagramParts.filter(function() {
            return $(this).index() <= targetIndex;
          }).fadeIn('slow');
          $diagramParts.filter(function() {
            return $(this).index() > targetIndex;
          }).fadeOut('slow');


          setTimeout(function() {
            if (!$targetStep.find('input[type=radio]:checked').size()) {
              $targetStep.find('input[type=radio]:first').click();
            }
          }, 50);
        };

        // Events
        $wizard.click(function(event) {
          var $target = $(event.target);

          // Next button
          if ($target.closest('div.button.next').size()) {
            if (!$form.valid()) return false;

            showStep($steps.filter(':visible').index() + 2);

            return false;
          }

          // Previous button
          if ($target.closest('div.button.previous').size()) {
            var index = $steps.filter(':visible').index();
            if (index) showStep(index);

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
            $wizard.find('div.data-disk-offering span.custom-disk-size').html(
              ui.value
            );
          }
        });

        return $wizard.dialog({
          title: 'Add instance',
          width: 800,
          height: 570,
          zIndex: 5000
        })
          .closest('.ui-dialog').overlay();
      };

      instanceWizard(args);
    };
  };
})(jQuery, cloudStack);
