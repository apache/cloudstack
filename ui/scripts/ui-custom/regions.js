(function($, cloudStack) {
  $(window).bind('cloudStack.ready', function() {
    // Region switcher
    var $regionList = $('<ul>');

    // Get region listing
    var refreshRegions = function() {
      $regionList.find('li').remove();
      cloudStack.sections.regions.regionSelector.dataProvider({
        response: {
          success: function(args) {
            var data = args.data;

            $(data).each(function() {
              var region = this;
              var $li = $('<li>').append($('<span>').html(_s(region.name)));

              $li.data('region-data', region);
              $regionList.append($li);
            });
          }
        }
      });
    };

    $(window).bind('cloudStack.refreshRegions', refreshRegions);

    var $regionSelector = $('<div>').addClass('region-selector')
    .append($('<div>').addClass('top-arrow'))
    .append($('<h2>').html(_l('label.menu.regions')))
    .append($regionList)
    .append(
      $('<div>').addClass('buttons')
      .append(
        $('<div>').addClass('button close').append($('<span>').html(_l('label.close')))
      )
    )
    .hide();
    var $regionSwitcherButton = $('<div>').addClass('region-switcher')
    .attr('title', 'Select region')
    .append(
      $('<span>').addClass('icon').html('&nbsp;')
    );

    var closeRegionSelector = function(args) {
      $regionSwitcherButton.removeClass('active');
      $regionSelector.fadeOut(args ? args.complete : null);
      $('body > .overlay').fadeOut(function() { $('body > .overlay').remove() });
    };

    var switchRegion = function(url) {

      closeRegionSelector({
        complete: function() {
          $('#container').prepend($('<div>').addClass('loading-overlay'));

          document.location.href = url;
        }
      });
    };

    $regionList.click(function(event) {
      var $target = $(event.target);
      var $li = $target.closest('li');

      if ($li.size()) {
        var url = $li.data('region-data').endpoint;

        switchRegion(url);
      }
    });

    $regionSwitcherButton.click(function() {
      if ($regionSwitcherButton.hasClass('active')) {
        closeRegionSelector();
      } else {
        $regionSwitcherButton.addClass('active');
        $regionSelector.fadeIn('fast').overlay({ closeAction: closeRegionSelector });
      }
    });

    $regionSelector.find('.button.close').click(function() {
      closeRegionSelector();
    });
    
    $('#header .controls .view-switcher.button:last').after($regionSwitcherButton, $regionSelector); 
    refreshRegions();
  });
}(jQuery, cloudStack));      
        
