(function($, cloudStack) {
  cloudStack.uiCustom.dashboard = function() {
    var $dashboard = $('#template').find('div.dashboard.admin').clone();

    $dashboard.find('.view-all').click(function() {
      $('#navigation li.events').click();
    });

    var getData = function() {
      // Populate data
      $dashboard.find('[data-item]').hide();
      cloudStack.sections.dashboard.dataProvider({
        response: {
          success: function(args) {
            var data = args.data;

            $.each(data, function(key, value) {
              var $elem = $dashboard.find('[data-item=' + key + ']');

              if ($elem.is('ul')) {
                $elem.show();
                var $liTmpl = $elem.find('li').remove();
                $(value).each(function() {
                  var item = this;
                  var $li = $liTmpl.clone().appendTo($elem).hide();

                  $.each(item, function(arrayKey, arrayValue) {
                    var $arrayElem = $li.find('[data-list-item=' + arrayKey + ']');

                    $arrayElem.html(arrayValue.substring(0, 50).concat('...'));
                  });

                  $li.attr({ title: item.description });

                  $li.fadeIn();
                });
              } else {
                $elem.each(function() {
                  var $item = $(this);
                  if ($item.hasClass('chart-line')) {
                    $item.show().animate({ width: value + '%' });
                  } else {
                    $item.hide().html(value).fadeIn();
                  }
                }); 
              }
            });
          }
        }
      });
    };

    getData();

    return $dashboard;
  }
}(jQuery, cloudStack));