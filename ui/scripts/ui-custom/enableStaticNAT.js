(function(cloudStack, $) {
  cloudStack.uiCustom.enableStaticNAT = function(args) {
    var listView = args.listView;
    var action = args.action;
    
    return function(args) {
      var context = args.context;
      var $instanceRow = args.$instanceRow;
      var vmList = function(args) {
        // Create a listing of instances, based on limited information
        // from main instances list view
        var $listView;
        var instances = $.extend(true, {}, args.listView, {
          uiCustom: true
        });

        instances.listView.actions = {
          select: {
            label: 'Select instance',
            type: 'radio',
            action: {
              uiCustom: function(args) {
                var $item = args.$item;
                var $input = $item.find('td.actions input:visible');

                if ($input.attr('type') == 'checkbox') {
                  if ($input.is(':checked'))
                    $item.addClass('multi-edit-selected');
                  else
                    $item.removeClass('multi-edit-selected');
                } else {
                  $item.siblings().removeClass('multi-edit-selected');
                  $item.addClass('multi-edit-selected');
                }
              }
            }
          }
        };

        $listView = $('<div>').listView(instances);

        // Change action label
        $listView.find('th.actions').html('Select');

        return $listView;
      };

      var $dataList = vmList({
        listView: listView
      }).dialog({
        dialogClass: 'multi-edit-add-list panel',
        width: 825,
        title: 'Select VM for Static NAT',
        buttons: [
          {
            text: 'Apply',
            'class': 'ok',
            click: function() {
              if (!$dataList.find(
                'input[type=radio]:checked, input[type=checkbox]:checked'
              ).size()) {
                cloudStack.dialog.notice({ message: 'Please select an instance '});
                
                return false;
              }

              var complete = args.complete;
              
              $dataList.fadeOut(function() {
                action({
                  context: $.extend(true, {}, context, {
                    instances: [
                      $dataList.find('tr.multi-edit-selected').data('json-obj')
                    ]
                  }),
                  response: {
                    success: function(args) {
                      complete({
                        $item: $instanceRow
                      });
                    }
                  }
                });
                $dataList.remove();
              });

              $('div.overlay').fadeOut(function() {
                $('div.overlay').remove();
              });
            }
          },
          {
            text: 'Cancel',
            'class': 'cancel',
            click: function() {
              $dataList.fadeOut(function() {
                $dataList.remove();
              });
              $('div.overlay').fadeOut(function() {
                $('div.overlay').remove();
              });
            }
          }
        ]
      }).parent('.ui-dialog').overlay();
    };
  };
}(cloudStack, jQuery));