(function($, cloudStack) {
  var elems = {
    // Side navigation bar
    navigation: function() {
      var $navigation = $('<div>').addClass('main-navigation');
      var $categories=$('<div>').addClass('button-category').html('CATEGORIES');

      $categories.appendTo($navigation);

    //  $navigation.html('Navigation');
      
      return $navigation;
    },

    // Listing 'tile' container
    listing: function() {
      var $listing = $('<div>').addClass('listing');
      var $categories=$('<div>').addClass('button-application').html('APPLICATIONS');
      var apps = cloudStack.marketplace.apps;
      
      $categories.appendTo($listing);

      //   $listing.html('Listing area');
      var $listPanel = $('<div>').addClass('list-panel').html('Application Development').hide();
        $listPanel.appendTo($listing);  
      var $listPanel2 = $('<div>').addClass('list-panel').html('Operations Management').hide();
      $listPanel2.appendTo($listing);
      var $listPanel3 = $('<div>').addClass('list-panel').html('Business Applications').hide();
      $listPanel3.appendTo($listing);
      var $contributePanel = $('<div>').addClass('contribute-list-panel').html('WOULD YOU LIKE TO <b></b>CONTRIBUTE ?');
       $contributePanel.appendTo($listing);
      var $mainPanel = $('<div>').addClass('main-panel');
      $mainPanel.appendTo($listing);
      var $panelFooter = $('<div>').addClass('footer-panel').html('&nbsp CloudStack 2012');
      $panelFooter.appendTo($listing);
      
      $.each(apps, function(key, value) {
        var appID = key;
        var app = value;
        var $miniPanel = $('<div>').addClass('login-panel');
        var $thumbnail = $('<div>').addClass('thumbnail');
        var thumbnailURL = 'url(' + cloudStack.marketplace.baseURL + '/apps/' + appID + '/images/thumbnail.png)';
        var configURL = cloudStack.marketplace.baseURL + '/apps/' + appID + '/config.js';

        $miniPanel.addClass(appID);
        $miniPanel.appendTo($mainPanel);
        $thumbnail.css({
          'background-image': thumbnailURL
        });
        $thumbnail.appendTo($miniPanel);

        $thumbnail.click(function() {
          require([configURL], function() {
            // fade-out main panel
            $listing.fadeOut(function() {
              var appConfig = cloudStack.marketplace.apps[appID];
              //debugger;
              // show detail panel
              var $detailView = $('<div>').addClass('app-content-detail-view');
              $detailView.appendTo('.marketplace-container');
              $detailView.show();
              $detailView.append($('<div>').addClass('config-title').append(appConfig.title).append("<br></br>")); 
              $detailView.append($('<div>').addClass('config-vendor').append(appConfig.vendor).append("<br></br>"));
              $detailView.append($('<div>').addClass('config-desc').append(appConfig.desc).append("<br></br>"));
              $detailView.append($('<div>').addClass('config-support-desc').append(appConfig.supportDesc).append("<br></br>"));
              $detailView.append($('<div>').addClass('config-support-contact').append(appConfig.supportContact));
                       
            /*  $.each(appConfig,function(key,value) {
                   $detailView.append(value).append("<br></br>");
                   
               });*/

           });
          });
        });
      });

      return $listing;
    },
    
    // Container for marketplace body
    // -- holds the navigation and listing area
    marketplace: function() {
      var $marketplace = $('<div>').addClass('marketplace-container');
      var $navigation = elems.navigation();
      var $listing = elems.listing();

      $marketplace.append($navigation, $listing);
      
      return $marketplace;
    },
    
    sectionSwitcher: function() {
      var $switcher = $('<div>').addClass('section-switcher');
      var $applications = $('<div>').addClass('buttons').html('Applications');
      var $templates = $('<div>').addClass('buttons').html('Templates');
      var $isos = $('<div>').addClass('buttons').html('ISOs');
 
      $applications.appendTo($switcher);
       $templates.appendTo($switcher);
       $isos.appendTo($switcher);

      // Append buttons
      // ...

      return $switcher;
    }
  };
  
  // Replace header items with marketplace-specific ones
  var updateHeader = function(args) {
    var $header = args.$header.clone();
    var $marketplaceSections = elems.sectionSwitcher();
    var $buttons = $header.find('.button'); // Notifications and project switcher
        
    // Hide buttons, show marketplace switcher in place
    $buttons
      .hide()
      .filter(':last')
      .after($marketplaceSections);
    
    $header.addClass('marketplace'); // Logo CSS will show different logo if .marketplace is present

    return $header;
  };

  cloudStack.uiCustom.marketplace = function(args) {
    var $header = $('#header');
    var $marketplaceHeader = updateHeader({
      $header: $header
    });
    var $container = $('#browser');
    var $navigation = $('#navigation');
    var $marketplace = elems.marketplace();

    // Update header
    $header.replaceWith($marketplaceHeader);

    // Replace CloudStack main UI
    $marketplace.hide();
    $navigation.fadeOut();
    $container.fadeOut(function() {
      $container.after($marketplace);
      $marketplace.show();
    });

    // Dummy placeholder
    return $('<div>');
  };
}(jQuery, cloudStack));
