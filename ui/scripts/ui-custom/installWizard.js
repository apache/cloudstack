(function($, cloudStack, testData) {
  var installWizard = function(args) {
    var context = args.context;
    var $installWizard = $('<div>').addClass('install-wizard');
    var $container = args.$container;
    var state = {}; // Hold wizard form state

    /**
     * Successful installation action
     */
    var complete = function() {
      $installWizard.remove();

      args.complete();
    };

    /**
     * Retrive copy text and append to element -- async
     * @param id
     * @param $elem
     */
    var getCopy = function(id, $elem) {
      cloudStack.installWizard.copy[id]({
        response: {
          success: function(args) {
            $elem.append(args.text);
          }
        }
      });

      return $elem;
    };

    /**
     * Go to specified step in flow -- for use in individual steps
     * @param stateStepID ID to group state elements in (i.e., zone, pod, cluster, ...)
     * @param $elem (optional) Element containing <form>, to serialize for state
     */
    var goTo = cloudStack._goto = function(stepID, stateID, $elem) {
      var $nextStep = steps[stepID]();
      var $body = $installWizard.find('.body');

      if (stateID && $elem) {
        state[stateID] = cloudStack.serializeForm($elem.is('form') ? $elem : $elem.find('form'));
      }

      $body.children().fadeOut('fast', function() {
        $(this).remove();
        $nextStep.addClass('step').hide().appendTo($body).fadeIn();
      });
    };

    /**
     * Generic page elements
     */
    var elems = {
      tooltip: function(title, content) {
        return $('<div>').addClass('tooltip-info').append(
          $('<div>').addClass('arrow'),
          $('<div>').addClass('title').html(title),
          $('<div>').addClass('content').append($('<p>').html(content))
        );
      },
      header: function() {
        return $('<div></div>').addClass('header')
          .append(
            $.merge(
              $('<h2></h2>').html('Hello and Welcome to CloudStack.'),
              $('<h3></h3>').html('This tour will aid you in setting up your CloudStack installation')
            )
        );
      },
      body: function() {
        return $('<div></div>').addClass('body');
      },
      nextButton: function(label, options) {
        var $button = options && !options.type ?
          $('<div>').addClass('button goTo').html(label) :
          $('<input>').attr({ type: 'submit' }).addClass('button goTo').val(label);

        return $button;
      },

      diagramParts: function() {
        return $('<div>').addClass('diagram').append(
          $('<div>').addClass('part zone'),
          $('<div>').addClass('part pod'),
          $('<div>').addClass('part cluster'),
          $('<div>').addClass('part host'),
          $('<div>').addClass('part primaryStorage'),
          $('<div>').addClass('part secondaryStorage'),
          $('<div>').addClass('part loading').append($('<div>').addClass('icon'))
        );
      }
    };

    var $diagramParts = elems.diagramParts();

    var showDiagram = function(parts) {
      $diagramParts.children().hide();
      $diagramParts.find(parts).show();
    };

    /**
     * Show tooltip for focused form elements
     */
    var showTooltip = function($formContainer, sectionID) {
      var $tooltip = elems.tooltip('Hints', '');

      $formContainer.find('input').focus(function() {
        var $input = $(this);

        $tooltip.find('p').html('');
        $tooltip.appendTo($formContainer);
        $tooltip.css({
          top: $(this).position().top - 20
        });
        
        var content = getCopy(
          'tooltip.' + sectionID + '.' + $input.attr('name'),
          $tooltip.find('p')
        );
      });

      $formContainer.find('input').blur(function() {
        $tooltip.remove();
      });

      setTimeout(function() {
        $formContainer.find('input:first').focus();        
      }, 600);
    };

    /**
     * Layout/behavior for each step in wizard
     */
    var steps = {
      changeUser: function(args) {
        var $changeUser = $('<div></div>').addClass('step change-user');
        var $form = $('<form></form>').appendTo($changeUser);

        // Fields
        var $password = $('<input>').addClass('required').attr({ type: 'password', name: 'password' });
        var $passwordConfirm = $('<input>').addClass('required').attr({ type: 'password', name: 'password-confirm' });
        var $save = elems.nextButton('Save and continue', { type: 'submit' });

        $form.append(
          $('<div></div>').addClass('title').html('Please change your password.'),
          $('<div></div>').addClass('field').append(
            $('<label>New Password:</label>'), $password
          ),
          $('<div></div>').addClass('field').append(
            $('<label>Confirm Password:</label>'), $passwordConfirm
          ),
          $save
        );

        $form.validate();

        // Save event
        $form.submit(function() {
          if (!$form.valid()) return false;

          var $loading = $('<div></div>').addClass('loading-overlay').prependTo($form);
          cloudStack.installWizard.changeUser({
            data: cloudStack.serializeForm($form),
            response: {
              success: function(args) {
                goTo('intro', 'newUser', $form);
              }
            }
          });

          return false;
        });

        return $changeUser;
      },

      intro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
          .html('What is CloudStack&#8482?');
        var $subtitle = $('<div></div>').addClass('subtitle')
          .html('Introduction to CloudStack&#8482');
        var $copy = getCopy('whatIsCloudStack', $('<p></p>'));
        var $continue = elems.nextButton('Continue with basic installation');
        var $advanced = elems.nextButton('Setup advanced installation').addClass('advanced-installation');

        $continue.click(function() {
          goTo('addZoneIntro');

          return false;
        });

        $advanced.click(function() {
          complete();
          
          return false;
        });

        return $intro.append(
          $title, $subtitle,
          $copy,
          $advanced,
          $continue
        );
      },

      /**
       * Add zone intro text
       * @param args
       */
      addZoneIntro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
          .html('Let\'s add a zone.');
        var $subtitle = $('<div></div>').addClass('subtitle')
          .html('What is a zone?');
        var $copy = getCopy('whatIsAZone', $('<p></p>'));
        var $continue = elems.nextButton('OK');

        $continue.click(function() {
          goTo('addZone');

          return false;
        });

        showDiagram('.part.zone');

        return $intro.append(
          $title, $subtitle,
          $copy,
          $continue
        );
      },

      /**
       * Add zone form
       * @param args
       */
      addZone: function(args) {
        var $addZone = $('<div></div>').addClass('add-zone');
        var $addZoneForm = $('<div>').addClass('setup-form').append(
          $('#template').find('.multi-wizard.zone-wizard .steps .setup-zone').clone()
        );
        var $save = elems.nextButton('Continue', { type: 'submit' });
        var $title = $('<div></div>').addClass('title').html('Setup Zone');

        $addZoneForm.find('form').validate();

        $save.click(function() {
          if (!$addZoneForm.find('form').valid()) return false;

          goTo('addIPRange', 'zone', $addZoneForm);

          return false;
        });

        // Remove unneeded fields
        $addZoneForm.find('.main-desc, .conditional').remove();
        $addZoneForm.find('.field:last').remove();

        showDiagram('.part.zone');
        showTooltip($addZoneForm, 'addZone');

        return $addZone.append(
          $addZoneForm
            .prepend($title)
            .append($save)
        );
      },

      /**
       * Add IP range form
       * @param args
       */
      addIPRange: function(args) {
        var $addIPRange = $('<div></div>').addClass('add-zone');
        var $addIPRangeForm = $('<div>').addClass('setup-form').append(
          $('#template').find('.multi-wizard.zone-wizard .steps .add-ip-range').clone()
        );
        var $save = elems.nextButton('Continue', { type: 'submit' });
        var $title = $('<div></div>').addClass('title').html('Setup IP Range');

        $addIPRangeForm.find('form').validate();

        $save.click(function() {
          if (!$addIPRangeForm.find('form').valid()) return false;

          goTo('addPodIntro', 'zoneIPRange', $addIPRangeForm);

          return false;
        });

        showDiagram('.part.zone');
        showTooltip($addIPRangeForm, 'addIPRange');

        // Remove unneeded fields
        $addIPRangeForm.find('.main-desc, .conditional').remove();

        return $addIPRange.append(
          $addIPRangeForm
            .prepend($title)
            .append($save)
        );
      },

      /**
       * Add pod intro text
       * @param args
       */
      addPodIntro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
          .html('Let\'s add a pod.');
        var $subtitle = $('<div></div>').addClass('subtitle')
          .html('What is a pod?');
        var $copy = getCopy('whatIsAPod', $('<p></p>'));
        var $continue = elems.nextButton('OK');

        $continue.click(function() {
          goTo('addPod');

          return false;
        });

        showDiagram('.part.zone, .part.pod');

        return $intro.append(
          $title, $subtitle,
          $copy,
          $continue
        );
      },

      /**
       * Add pod form
       * @param args
       */
      addPod: function(args) {
        var $addPod = $('<div></div>').addClass('add-pod');
        var $addPodForm = $('<div>').addClass('setup-form').append(
          $('#template').find('.multi-wizard.zone-wizard .steps .setup-pod').clone()
        );
        var $save = elems.nextButton('Continue', { type: 'submit' });
        var $title = $('<div></div>').addClass('title').html('Add a Pod');

        $addPodForm.find('form').validate();

        $save.click(function() {
          if (!$addPodForm.find('form').valid()) return false;

          goTo('addClusterIntro', 'pod', $addPodForm);

          return false;
        });

        // Remove unneeded fields
        $addPodForm.find('.main-desc, .conditional').remove();

        showDiagram('.part.zone, .part.pod');
        showTooltip($addPodForm, 'addPod');

        return $addPod.append(
          $addPodForm
            .prepend($title)
            .append($save)
        );
      },

      /**
       * Add cluster intro text
       * @param args
       */
      addClusterIntro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
          .html('Let\'s add a cluster.');
        var $subtitle = $('<div></div>').addClass('subtitle')
          .html('What is a cluster?');
        var $copy = getCopy('whatIsACluster', $('<p></p>'));
        var $continue = elems.nextButton('OK');

        $continue.click(function() {
          goTo('addCluster');

          return false;
        });

        showDiagram('.part.zone, .part.cluster');

        return $intro.append(
          $title, $subtitle,
          $copy,
          $continue
        );
      },

      /**
       * Add cluster form
       * @param args
       */
      addCluster: function(args) {
        var $addCluster = $('<div></div>').addClass('add-cluster');
        var addClusterForm = cloudStack.dialog.createForm({
          context: {
            zones: [{}]
          },
          noDialog: true,
          form: cloudStack.sections.system
            .subsections.clusters.listView
            .actions.add.createForm,
          after: function(args) {
            goTo('addHostIntro', 'cluster', $addClusterForm);
          }
        });
        var $addClusterForm = $('<div>').addClass('setup-form').append(
          addClusterForm.$formContainer
        );

        var $save = elems.nextButton('Continue', { type: 'submit' }).appendTo($addClusterForm.find('form'));
        var $title = $('<div></div>').addClass('title').html('Add a Cluster');

        $addClusterForm.find('form').submit(function() {
          addClusterForm.completeAction($addClusterForm);

          return false;
        });

        showDiagram('.part.zone, .part.cluster');
        showTooltip($addClusterForm, 'addCluster');

        // Cleanup
        $addClusterForm.find('.message').remove();
        $addClusterForm.find('.form-item').addClass('field').find('label.error').hide();
        $addClusterForm.find('.form-item[rel=podId]').remove();

        return $addCluster.append(
          $addClusterForm
            .prepend($title)
        );
      },

      /**
       * Add host intro text
       * @param args
       */
      addHostIntro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
          .html('Let\'s add a host.');
        var $subtitle = $('<div></div>').addClass('subtitle')
          .html('What is a host?');
        var $copy = getCopy('whatIsAHost', $('<p></p>'));
        var $continue = elems.nextButton('OK');

        $continue.click(function() {
          goTo('addHost');

          return false;
        });

        showDiagram('.part.zone, .part.host');

        return $intro.append(
          $title, $subtitle,
          $copy,
          $continue
        );
      },

      /**
       * Add host form
       * @param args
       */
      addHost: function(args) {
        var $addHost = $('<div></div>').addClass('add-host');
        var addHostForm = cloudStack.dialog.createForm({
          context: { zones: [{}] },
          noDialog: true,
          form: {
            title: 'Add new host',
            desc: 'Please fill in the following information to add a new host fro the specified zone configuration.',
            fields: {
              hostname: {
                label: 'Host name',
                validation: { required: true }
              },

              username: {
                label: 'User name',
                validation: { required: true }
              },

              password: {
                label: 'Password',
                validation: { required: true },
                isPassword: true
              },
              //always appear (begin)
              hosttags: {
                label: 'Host tags',
                validation: { required: false }
              }
              //always appear (end)
            }
          },
          after: function(args) {
            goTo('addPrimaryStorageIntro', 'host', $addHostForm);
          }
        });
        var $addHostForm = $('<div>').addClass('setup-form').append(
          addHostForm.$formContainer
        );
        var $save = elems.nextButton('Continue', { type: 'submit' }).appendTo($addHostForm.find('form'));
        var $title = $('<div></div>').addClass('title').html('Add a Host');

        $addHostForm.find('form').submit(function() {
          addHostForm.completeAction($addHostForm);

          return false;
        });

        showDiagram('.part.zone, .part.host');
        showTooltip($addHostForm, 'addHost');

        // Cleanup
        $addHostForm.find('.message').remove();
        $addHostForm.find('.form-item').addClass('field').find('label.error').hide();
        $addHostForm.find('.form-item[rel=cluster], .form-item[rel=pod]').remove();

        return $addHost.append(
          $addHostForm
            .prepend($title)
        );
      },

      /**
       * Add primary storage intro text
       * @param args
       */
      addPrimaryStorageIntro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
              .html('Let\'s add primary storage.');
        var $subtitle = $('<div></div>').addClass('subtitle')
              .html('What is primary storage?');
        var $copy = getCopy('whatIsPrimaryStorage', $('<p></p>'));
        var $continue = elems.nextButton('OK');

        $continue.click(function() {
          goTo('addPrimaryStorage');

          return false;
        });

        showDiagram('.part.zone, .part.primaryStorage');

        return $intro.append(
          $title, $subtitle,
          $copy,
          $continue
        );
      },

      /**
       * Add primary storage
       * @param args
       */
      addPrimaryStorage: function(args) {
        var $addPrimaryStorage = $('<div></div>').addClass('add-primary-storage');
        var addPrimaryStorageForm = cloudStack.dialog.createForm({
          noDialog: true,
          form: {
            title: 'Add new primary storage',
            desc: 'Please fill in the following information to add a new primary storage',
            fields: {
              name: {
                label: 'Name',
                validation: { required: true }
              },

              server: {
                label: 'Server',
                validation: { required: true }
              },

              path: {
                label: 'Path',
                validation: { required: true }
              },

              storageTags: {
                label: 'Storage Tags',
                validation: { required: false }
              }
            }
          },
          after: function(args) {
            goTo('addSecondaryStorageIntro', 'primaryStorage', $addPrimaryStorageForm);
          }
        });
        var $addPrimaryStorageForm = $('<div>').addClass('setup-form').append(
          addPrimaryStorageForm.$formContainer
        );
        var $save = elems.nextButton('Continue', { type: 'submit' }).appendTo($addPrimaryStorageForm.find('form'));
        var $title = $('<div></div>').addClass('title').html('Add Primary Storage');

        $addPrimaryStorageForm.find('form').submit(function() {
          addPrimaryStorageForm.completeAction($addPrimaryStorageForm);

          return false;
        });

        showDiagram('.part.zone, .part.primaryStorage');
        showTooltip($addPrimaryStorageForm, 'addPrimaryStorage');

        // Cleanup
        $addPrimaryStorageForm.find('.message').remove();
        $addPrimaryStorageForm.find('.form-item').addClass('field').find('label.error').hide();
        $addPrimaryStorageForm.find('.form-item[rel=clusterId], .form-item[rel=podId]').remove();

        return $addPrimaryStorage.append(
          $addPrimaryStorageForm
            .prepend($title)
        );
      },

      /**
       * Add secondary storage intro text
       * @param args
       */
      addSecondaryStorageIntro: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
              .html('Let\'s add secondary storage.');
        var $subtitle = $('<div></div>').addClass('subtitle')
              .html('What is a secondary storage?');
        var $copy = getCopy('whatIsSecondaryStorage', $('<p></p>'));
        var $continue = elems.nextButton('OK');

        $continue.click(function() {
          goTo('addSecondaryStorage');

          return false;
        });

        showDiagram('.part.zone, .part.secondaryStorage');

        return $intro.append(
          $title, $subtitle,
          $copy,
          $continue
        );
      },

      /**
       * Add secondary storage
       * @param args
       */
      addSecondaryStorage: function(args) {
        var $addSecondaryStorage = $('<div></div>').addClass('add-secondary-storage');
        var addSecondaryStorageForm = cloudStack.dialog.createForm({
          noDialog: true,
          form: {
            title: 'Add new secondary storage',
            desc: 'Please fill in the following information to add a new secondary storage',
            fields: {
              nfsServer: {
                label: 'NFS Server',
                validation: { required: true }
              },
              path: {
                label: 'Path',
                validation: { required: true }
              }
            }
          },
          after: function(args) {
            goTo('launchInfo', 'secondaryStorage', $addSecondaryStorageForm);
          }
        });
        var $addSecondaryStorageForm = $('<div>').addClass('setup-form').append(
          addSecondaryStorageForm.$formContainer
        );
        var $save = elems.nextButton('Continue', { type: 'submit' }).appendTo($addSecondaryStorageForm.find('form'));
        var $title = $('<div></div>').addClass('title').html('Add Secondary Storage');

        $addSecondaryStorageForm.find('form').submit(function() {
          addSecondaryStorageForm.completeAction($addSecondaryStorageForm);

          return false;
        });

        showDiagram('.part.zone, .part.secondaryStorage');
        showTooltip($addSecondaryStorageForm, 'addSecondaryStorage');

        // Cleanup
        $addSecondaryStorageForm.find('.message').remove();
        $addSecondaryStorageForm.find('.form-item').addClass('field').find('label.error').hide();

        return $addSecondaryStorage.append(
          $addSecondaryStorageForm
            .prepend($title)
        );
      },

      /**
       * Pre-launch text
       */
      launchInfo: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
          .html('Congratulations!.');
        var $subtitle = $('<div></div>').addClass('subtitle')
          .html('Click the launch button.');
        var $continue = elems.nextButton('Launch');

        $continue.click(function() {
          goTo('launch');

          return false;
        });

        showDiagram('.part.zone, .part.secondaryStorage');

        return $intro.append(
          $title, $subtitle,
          $continue
        );
      },

      /**
       * Initiates launch tasks
       */
      launch: function(args) {
        var $intro = $('<div></div>').addClass('intro');
        var $title = $('<div></div>').addClass('title')
              .html('Now building your cloud...');
        var $subtitle = $('<div></div>').addClass('subtitle')
              .html('You may want to get a cup of coffee right now.');

        cloudStack.installWizard.action({
          data: state,
          response: {
            success: function() {
              complete();
            }
          }
        });

        showDiagram('.part.loading');

        return $intro.append(
          $title, $subtitle
        );
      }
    };

    var initialStep = steps.changeUser().addClass('step');

    $installWizard.append(
      elems.header(),
      elems.body().append(initialStep),
      $diagramParts
    ).appendTo($container);
  };

  cloudStack.uiCustom.installWizard = installWizard;
}(jQuery, cloudStack, testData));
