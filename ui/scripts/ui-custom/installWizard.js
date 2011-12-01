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
      /**
       * A standard wizard step template
       * -- relies on createForm for form generation
       */
      step: function(args) {
        var title = args.title;
        var formData = args.form;
        var diagram = args.diagram;
        var id = args.id;
        var stateID = args.stateID;
        var tooltipID = args.tooltipID;
        var nextStepID = args.nextStepID;
        var form;

        var $container = $('<div></div>').addClass(id);
        var $form = $('<div>').addClass('setup-form');
        var $save = elems.nextButton('Continue', { type: 'submit' });
        var $title = $('<div></div>').addClass('title').html(title);

        // Generate form
        form = cloudStack.dialog.createForm({
          noDialog: true,
          form: {
            title: title,
            desc: '',
            fields: formData
          },
          after: function(args) {
            goTo(nextStepID, stateID, $form);
          }
        });

        $form.append(form.$formContainer);
        $form.find('.form-item').addClass('field');
        $save.appendTo($form.find('form'));

        // Submit handler
        $form.find('form').submit(function() {
          form.completeAction($form);

          return false;
        });

        // Setup diagram, tooltips
        showDiagram(diagram);

        // Cleanup
        $form.find('.message').remove();
        $form.find('label.error').hide();

        $container.append($form.prepend($title));
        
        return function(args) {
          showTooltip($form, tooltipID);

          return $container;
        };
      },

      /**
       * A form item tooltip
       */
      tooltip: function(title, content) {
        return $('<div>').addClass('tooltip-info').append(
          $('<div>').addClass('arrow'),
          $('<div>').addClass('title').html(title),
          $('<div>').addClass('content').append($('<p>').html(content))
        );
      },

      /**
       * The main header
       */
      header: function() {
        return $('<div></div>').addClass('header')
          .append(
            $.merge(
              $('<h2></h2>').html('Hello and Welcome to CloudStack.'),
              $('<h3></h3>').html('This tour will aid you in setting up your CloudStack installation')
            )
        );
      },

      /**
       * The wizard body (contains form)
       */
      body: function() {
        return $('<div></div>').addClass('body');
      },

      /**
       * A standard next button
       */
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
      intro: function(args) {
        var $intro = $('<div></div>').addClass('intro what-is-cloudstack');
        var $title = $('<div></div>').addClass('title')
              .html('What is CloudStack&#8482?');
        var $subtitle = $('<div></div>').addClass('subtitle')
              .html('Introduction to CloudStack&#8482');
        var $copy = getCopy('whatIsCloudStack', $('<p></p>'));
        var $continue = elems.nextButton('Continue with basic installation');
        var $advanced = elems.nextButton(
          'I have used Cloudstack before, skip this guide'
        ).addClass('advanced-installation');

        $continue.click(function() {
          goTo('changeUser');

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
                goTo('addZoneIntro', 'newUser', $form);
              }
            }
          });

          return false;
        });

        return $changeUser;
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
       */
      addZone: elems.step({
        title: 'Add zone',
        id: 'add-zone',
        stateID: 'zone',
        tooltipID: 'addZone',
        diagram: '.part.zone',
        nextStepID: 'addPodIntro',
        form: {
          name: { label: 'Name', validation: { required: true } },
          dns1: { label: 'DNS 1', validation: { required: true } },
          dns2: { label: 'DNS 2' },
          internaldns1: { label: 'Internal DNS 1', validation: { required: true } },
          internaldns2: { label: 'Internal DNS 2' }
        }
      }),

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
      addPod: elems.step({
        title: 'Add pod',
        id: 'add-pod',
        stateID: 'pod',
        tooltipID: 'addPod',
        diagram: '.part.zone, .part.pod',
        nextStepID: 'addGuestNetwork',
        form: {
          name: { label: 'Name', validation: { required: true }},
          gateway: { label: 'Gateway', validation: { required: true }},
          netmask: { label: 'Netmask', validation: { required: true }},
          ipRange: { label: 'IP Range', range: ['startip', 'endip'], validation: { required: true }}
        }
      }),

      /**
       * Add guest network form
       */
      addGuestNetwork: elems.step({
        title: 'Add guest network',
        id: 'add-guest-network',
        stateID: 'guestNetwork',
        tooltipID: 'launchInfo',
        diagram: '.part.zone',
        nextStepID: 'launchInfo',
        form: {
          name: { label: 'Name', validation: { required: true } },
          description: { label: 'Description', validation: { required: true } },
          guestGateway: { label: 'Gateway', validation: { required: true } },
          guestNetmask: { label: 'Netmask', validation: { required: true } },
          guestIPRange: { label: 'IP Range', range: ['guestStartIp', 'guestEndIp'], validation: { required: true } }
        }
      }),

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
      addCluster: elems.step({
        title: 'Add cluster',
        id: 'add-cluster',
        stateID: 'cluster',
        tooltipID: 'addCluster',
        nextStepID: 'addHostIntro',
        diagram: '.part.zone, .part.cluster',
        form: {
          hypervisor: {
            label: 'Hypervisor',
            select: function(args) {
              args.response.success({ data: [
                { id: 'xen', description: 'XenServer' }
              ]});
            }
          },
          name: { label: 'Name', validation: { required: true }}
        }
      }),

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
      addHost: elems.step({
        title: 'Add host',
        id: 'add-host',
        stateID: 'host',
        tooltipID: 'addHost',
        nextStepID: 'addPrimaryStorageIntro',
        diagram: '.part.zone, .part.host',
        form: {
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
          }
        }
      }),

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
      addPrimaryStorage: elems.step({
        title: 'Add primary storage',
        id: 'add-primary-storage',
        stateID: 'primaryStorage',
        tooltipID: 'addPrimaryStorage',
        nextStepID: 'addSecondaryStorageIntro',
        diagram: '.part.zone, .part.primaryStorage',
        form: {
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
      }),

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
      addSecondaryStorage: elems.step({
        title: 'Add secondary storage',
        id: 'add-secondary-storage',
        stateID: 'secondaryStorage',
        tooltipID: 'addSecondaryStorage',
        nextStepID: 'launchInfo',
        diagram: '.part.zone, .part.secondaryStorage',
        form: {
          nfsServer: {
            label: 'NFS Server',
            validation: { required: true }
          },
          path: {
            label: 'Path',
            validation: { required: true }
          }
        }
      }),

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

    var initialStep = steps.intro().addClass('step');
    showDiagram('');

    $installWizard.append(
      elems.header(),
      elems.body().append(initialStep),
      $diagramParts
    ).appendTo($container);
  };

  cloudStack.uiCustom.installWizard = installWizard;
}(jQuery, cloudStack, testData));
