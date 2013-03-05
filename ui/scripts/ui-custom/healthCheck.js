// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

(function($, cloudStack) {

  cloudStack.uiCustom.healthCheck = function(args) {

    // Place outer args here as local variables
    // i.e, -- var dataProvider = args.dataProvider

    return function(args){
   
     var context = args.context;
     var formData = args.formData;
     var forms = $.extend(true, {}, args.forms);
     var topFieldForm, bottomFieldForm , $topFieldForm , $bottomFieldForm;
     var topfields = forms.topFields; 

     var $healthCheckDesc = $('<div>Your load balancer will automatically perform health checks on your cloudstack instances and only route traffic to instances that pass the health check </div>').addClass('health-check-description');
     var $healthCheckConfigTitle = $('<div><br><br>Configuration Options :</div>').addClass('health-check-config-title');
     var $healthCheckAdvancedTitle = $('<div><br><br> Advanced Options : </div>').addClass('health-check-advanced-title');
    
     var $healthCheckDialog = $('<div>').addClass('health-check');
       
    
      $healthCheckDialog.append($healthCheckDesc);
      $healthCheckDialog.append($healthCheckConfigTitle);
    

      topFieldForm = cloudStack.dialog.createForm({
          context: context,
          noDialog: true, // Don't render a dialog, just return $formContainer
          form: {
            title: '',
            fields:{
               protocol: { label: 'Ping Protocol' , docID:'helpAccountUsername', validation :{required:true}, defaultValue: 'HTTP'},
               port : {label: 'Ping Port ', docID:'helpAccountUsername', validation: {required:true}, defaultValue: '80'},
               pingpath: {label: 'Ping Path', docID:'helpAccountUsername' , validation: {required: true}, defaultValue: '/' }
             }
          }
        });

       $topFieldForm = topFieldForm.$formContainer;
       $topFieldForm.appendTo($healthCheckDialog);

       $healthCheckDialog.append($healthCheckAdvancedTitle);


        bottomFieldForm = cloudStack.dialog.createForm ({
              context:context,
              noDialog:true,
              form:{
                title:'',
                fields:{
                   responsetimeout: {label: 'Response Timeout (in sec)' , validation:{required:false},defaultValue:'5'},
                   healthinterval: {label: 'Health Check Interval (in min)',  validation:{required:false}, defaultValue :'1'},
                   unhealthythreshold: {label: 'Unhealthy Threshold' , validation: { required:false}, defaultValue:'2'},
                   healthythreshold:  {label: 'Healthy Threshold', validation: {required:false} ,defaultValue:'10'}

                 }
             }
       });

       $bottomFieldForm = bottomFieldForm.$formContainer;
       $bottomFieldForm.appendTo($healthCheckDialog);


      //var $loading = $('<div>').addClass('loading-overlay').appendTo($healthCheckDialog);
      $healthCheckDialog.dialog({
        title: 'Health Check Wizard',
        width: 600,
        height: 600,
        draggable: true,
        closeonEscape: false,
        overflow:'auto',
        open:function() {
          $("button").each(function(){
            $(this).attr("style", "left: 400px; position: relative; margin-right: 5px; ");
          });
        },
        buttons: [
          {
            text: _l('label.cancel'),
            'class': 'cancel',
            click: function() {
              $healthCheckDialog.dialog('destroy');
               $('.overlay').remove();
            }
          },
          {
            text: _l('Apply'),
            'class': 'ok',
            click: function() {
            //  var data = cloudStack.serializeForm($('.ui-dialog .healthCheck form'));

              $loading.appendTo($healthCheckDialog);
          /*    cloudStack.autoscaler.actions.apply({
                formData: formData,
                context: context,
                data: data,
                response: {
                  success: function() {
                    $loading.remove();
                    $autoscalerDialog.dialog('destroy');
                    $autoscalerDialog.closest(':ui-dialog').remove();
                    $('.overlay').remove();
                    cloudStack.dialog.notice({
                      message: 'Autoscale configured successfully.'
                    });
                  },
                  error: function(message) {
                    cloudStack.dialog.notice({ message: message });
                    $loading.remove();
                  }
                }
              });*/
 
              }
          }
        ]
      }).closest('.ui-dialog').overlay();


    }
  }
 }(jQuery, cloudStack));


