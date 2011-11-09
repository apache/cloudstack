(function($, cloudStack, testData) {
  cloudStack.uiCustom.installWizard = function(args) {
    var context = args.context;
    var $installWizard = $('<div>').addClass('install-wizard');
    var $container = args.$container;

    var elems = {
      nextButton: function() {
        return $('<div>').addClass('button next').html('Next');
      }
    };

    // Layout/behavior for each step in wizard
    var steps = {
      // Welcome screen
      welcome: function(args) {
        return $.merge(
          $('<h1>').html('Welcome screen'),
          $('<p>').html('Welcome text goes here.'),
          $('<div>').addClass('button next').html('Next'),
          elems.nextButton().click(args.nextStep)
        );
      },

      addZone: function(args) {
        var $zoneWizard = $('#template').find('.multi-wizard.zone-wizard').clone();

        return $.merge(
          $zoneWizard.find('.steps .setup-zone'),
          elems.nextButton().click(function() {
            args.nextStep({
              data: {
                zone: cloudStack.serializeForm
              }
            });
          })
        );
      }
    };

    $installWizard.append(steps.addZone).appendTo($container);
  };
}(jQuery, cloudStack, testData));