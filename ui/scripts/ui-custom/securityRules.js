(function($, cloudStack) {
  cloudStack.uiCustom.securityRules = function(args) {
    var multiEdit = args;
    
    return function(args) {
      var context = args.context;
      var $multi = $('<div>').addClass('security-rules').multiEdit(
        $.extend(true, {}, multiEdit, {
          context: context
        })
      );
      var $fields = $multi.find('form table').find('th, td');
      var $accountFields = $fields.filter(function() {
        return $(this).hasClass('accountname') ||
          $(this).hasClass('securitygroupname');
      });
      var $cidrFields = $fields.filter(function() {
        return $(this).hasClass('cidr');
      });

      $multi.prepend(
        $('<div>').addClass('add-by')
          .append($('<span>').html('Add by:'))
          .append(
            $('<div>').addClass('selection')
              .append(
                $('<input>').attr({
                  type: 'radio',
                  name: 'add-by',
                  checked: 'checked'
                }).click(function() {
                  $accountFields.hide();
                  $cidrFields.show();

                  return true;
                }).click()
              )
              .append($('<label>').html('CIDR'))
              .append(
                $('<input>').attr({
                  type: 'radio',
                  name: 'add-by'
                }).click(function() {
                  $accountFields.show();
                  $cidrFields.hide();

                  return true;
                })
              )
              .append($('<label>').html('Account'))
          )
      );
      
      return $multi;
    };
  };
})(jQuery, cloudStack);
