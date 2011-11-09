(function($, cloudStack, testData) {
  cloudStack.installWizard = {
    // Check if install wizard should be invoked
    check: function(args) {
      $.ajax({
        url: createURL('listZones'),
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            doInstall: !(data.listzonesresponse.zone && data.listzonesresponse.zone.length)
          });          
        }
      });
    }
  };
}(jQuery, cloudStack, testData));