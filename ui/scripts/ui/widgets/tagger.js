(function($) {
  var elems = {
    tagItem: function(title, onRemove) {
      var $li = $('<li>');
      var $label = $('<span>').addClass('label').html(title);
      var $remove = $('<span>').addClass('remove').html('X');

      $remove.click(function() {
        $li.remove();
        
        if (onRemove) onRemove();
      });

      $li.append($remove, $label);
      
      return $li;
    }
  };
  
  $.widget('cloudStack.tagger', {
    _init: function() {
      var $container = $('<div>').addClass('tagger');
      var $tagArea = $('<ul>').addClass('tags');
      var $originalInput = this.element;
      var $input = $('<input>').attr('type', 'text');

      $originalInput.hide();
      $originalInput.after($container);
      $container.append($tagArea, $input);

      // Reposition input to fit tag list
      var relayout = function() {
        $input.width(
          $container.width() - $tagArea.width() - 25
        );
        $input.css({
          left: $tagArea.width(),
          top: $tagArea.position().top
        });
      };

      var onRemove = function() {
        syncInputs(true);
        relayout();
      };

      // sync original input box and tag list values
      //
      // flag == true: Sync tags->text
      // flag == false: Sync text->tags
      var syncInputs = function(flag) {
         if (flag) {
           $originalInput.val(
             $tagArea.find('li').map(function(index, tag) {
               return $(tag).find('span.label').html();
             }).toArray().join(',')
           );
         } else if ($originalInput.val()) {
           $($originalInput.val().split(',')).map(function(index, tag) {
             elems.tagItem(tag, onRemove).appendTo($tagArea);
           });

           $tagArea.show();
           relayout();
         }
      };

      // Tag detection (comma-delimited)
      $input.keypress(function(event) {
        var tagCode = 44; // Symbol used to indicate a new tag

        if (event.which == tagCode) {
          $tagArea.show();
          elems.tagItem($input.val(), onRemove).appendTo($tagArea);
          $input.val('');
          relayout();
          syncInputs(true);

          return false; // Don't allow delineator to be added to input box
        }

        return true;
      });

      $tagArea.hide();
      relayout();
      syncInputs(false);
    }
  });
}(jQuery));
