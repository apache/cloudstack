(function($) {
  /**
   * Create a dark overlay, for modal dialogs, etc.
   */
  $.fn.overlay = function(args) {
    var $topElem = this;
    var $overlay = $('<div class="overlay">').hide().appendTo('html body').css({
      position: 'absolute',
      background: 'black',
      opacity: 0.5,
      width: '100%',
      height: '100%',
      top: $(window).scrollTop(),
      left: 0,
      zIndex: $topElem.css('z-index') - 1
    }).show();

    // Events
    $overlay.click(function(event) {
      if (!args || !args.closeAction) return false; 

      args.closeAction();
      $overlay.fadeOut(function() {
        $overlay.remove();
      });
    });

    return this;
  };
})(window.jQuery);
