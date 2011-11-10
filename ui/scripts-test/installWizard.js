(function($, cloudStack, testData) {
  cloudStack.installWizard = {
    // Check if install wizard should be invoked
    check: function(args) {
      setTimeout(function() {
        args.response.success({
          doInstall: false
        });
      }, 100);
    }
  };
}(jQuery, cloudStack, testData));