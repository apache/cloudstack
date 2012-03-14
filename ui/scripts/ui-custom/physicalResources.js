(function(cloudStack, $) {
  cloudStack.uiCustom.physicalResources = function(args) {
    var listView = function() {
      return $('<div>').listView(args);
    };

    var $dashboard = $('#template').find('.system-dashboard-view').clone();

    var getData = function() {
      // Populate data
      $dashboard.find('[data-item]').hide();
      cloudStack.sections.system.dashboard.dataProvider({
        response: {
          success: function(args) {
            var data = args.data;

            $.each(data, function(key, value) {
              var $elem = $dashboard.find('[data-item=' + key + ']');
              $elem.hide().html(value).fadeIn();
            });
          }
        }
      });
    };

    var resourceChart = function(args) {
      getData();
      
      return $dashboard
        .click(function(event) {
          var $target = $(event.target);

          if ($target.closest('.view-more').size()) {
            args.$browser.cloudBrowser('addPanel', {
              title: 'Zones',
              data: '',
              noSelectPanel: true,
              maximizeIfSelected: true,
              complete: function($newPanel) {
                listView().appendTo($newPanel);
              }
            });
          }
        });
    };

    $(window).bind('cloudStack.fullRefresh cloudStack.updateResources', function() {
      if ($dashboard.is(':visible')) {
        getData();
      }
    });

    return function(args) {
      $dashboard.find('#update_ssl_button').click(function() {   	
				cloudStack.dialog.createForm({
					form: {
						title: 'label.update.ssl',	
            desc: 'message.update.ssl',						
						fields: {
							certificate: { label: 'label.certificate', isTextarea: true },
							privatekey: { label: 'label.privatekey', isTextarea: true },
							domainsuffix: { label: 'label.domain.suffix' }
						}
					},
					after: function(args) {					  
						var $loading = $('<div>').addClass('loading-overlay');
            $('.system-dashboard-view:visible').prepend($loading);
						
						$.ajax({
						  url: createURL('uploadCustomCertificate'),
							data: {
							  certificate: args.data.certificate,
								privatekey: args.data.privatekey,
								domainsuffix: args.data.domainsuffix
							},
							dataType: 'json',
							success: function(json) {							  				
								var jid = json.uploadcustomcertificateresponse.jobid;								
								var timerKey = "uploadcustomcertificatejob_" + jid;
								$("body").everyTime(2000, timerKey, function() {								  
									$.ajax({
										url: createURL("queryAsyncJobResult&jobId=" + jid),
										dataType: "json",
										success: function(json) {
										  
											var result = json.queryasyncjobresultresponse;
											if (result.jobstatus == 0) {
												return; //Job has not completed
											}
											else {
												$("body").stopTime(timerKey);
												if (result.jobstatus == 1) {
													cloudStack.dialog.notice({ message: 'Update SSL Certiciate succeeded'	});
												}
												else if (result.jobstatus == 2) {
													cloudStack.dialog.notice({ message: 'Failed to update SSL Certificate. ' + _s(result.jobresult.errortext) });													
												}
												$loading.remove();
											}
										},
										error: function(XMLHttpResponse) {											
											cloudStack.dialog.notice({ message: 'Failed to update SSL Certificate. ' + parseXMLHttpResponse(XMLHttpResponse) });
											$loading.remove();
										}
									});
								});				
							},              
							error: function(XMLHttpResponse) {											
								cloudStack.dialog.notice({ message: 'Failed to update SSL Certificate. ' + parseXMLHttpResponse(XMLHttpResponse) });
								$loading.remove();
							}
						});								 
					},				
					context: {}
				});				
                
        return false;
      });
			$dashboard.find('#refresh_button').click(function() {    	  
        getData();
        
        return false;
      });

      return resourceChart(args);
    };
  };
}(cloudStack, jQuery));