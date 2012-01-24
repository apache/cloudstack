(function($, cloudStack) {
  cloudStack.lbStickyPolicy = function(args) {
    return function(args) {
      var success = args.response.success;
      var context = args.context;
      var network = args.context.networks[0];

      var lbService = $.grep(network.service, function(service) {
        return service.name == 'Lb';
      })[0];
      var stickinessCapabilities = JSON.parse(
        $.grep(lbService.capability, function(capability) {
          return capability.name == 'SupportedStickinessMethods';
        })[0].value
      );

      var baseFields = {
        name: { label: 'Name', validation: { required: true }, isHidden: true },
        mode: { label: 'Mode', isHidden: true },
        length: { label: 'Length', validation: { required: true }, isHidden: true },
        holdtime: { label: 'Hold Time', validation: { required: true }, isHidden: true },
        tablesize: { label: 'Table size', isHidden: true },
        expire: { label: 'Expire', isHidden: true },
        requestlearn: { label: 'Request-Learn', isBoolean: true, isHidden: true },
        prefix: { label: 'Prefix', isBoolean: true, isHidden: true },
        nocache: { label: 'No cache', isBoolean: true, isHidden: true },
        indirect: { label: 'Indirect', isBoolean: true, isHidden: true },
        postonly: { label: 'Is post-only', isBoolean: true, isHidden: true },
        domain: { label: 'Domain', isBoolean: true, isHidden: true }
      };

      var conditionalFields = {
        methodname: {
          label: 'Stickiness method',
          select: function(args) {
            var $select = args.$select;
            var $form = $select.closest('form');
            
            args.response.success({
              data: $.map(stickinessCapabilities, function(stickyCapability) {
                return {
                  id: stickyCapability.methodname,
                  description: stickyCapability.methodname
                };
              })
            }, 500);

            $select.change(function() {
              var value = $select.val();
              var showFields = [];
              var targetMethod = $.grep(stickinessCapabilities, function(stickyCapability) {
                return stickyCapability.methodname == value;
              })[0];
              var visibleParams = $.map(targetMethod.paramlist, function(param) {
                return param.paramname
              });

              $select.closest('.form-item').siblings('.form-item').each(function() {
                var $field = $(this);
                var id = $field.attr('rel');

                if ($.inArray(id, visibleParams) > -1) {
                  $field.css('display', 'inline-block');
                } else {
                  $field.hide();
                }
              });

              $select.closest(':ui-dialog').dialog('option', 'position', 'center');
            });
          }
        }
      };
      
      var fields = $.extend(conditionalFields, baseFields);

      if (args.data) {
        var populatedFields = $.map(fields, function(field, id) {
          return id;
        });
        
        $(populatedFields).each(function() {
          var id = this;
          var field = fields[id];
          var dataItem = args.data[id];

          if (field.isBoolean) {
            field.isChecked = dataItem ? true : false;
          } else {
            field.defaultValue = dataItem;
          }
        });
      }

      cloudStack.dialog.createForm({
        form: {
          title: 'Configure Sticky Policy',
          desc: 'Please complete the following fields',
          fields: fields
        },
        after: function(args) {
          var data = cloudStack.serializeForm(args.$form);
          success({
            data: $.extend(data, {
              _buttonLabel: data.methodname.toUpperCase()
            })
          });
        }
      });
    };
  };
}(jQuery, cloudStack));
