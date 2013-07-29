angular.module('directives.confirm', ['ui.bootstrap.dialog']);
angular.module('directives.confirm').directive('confirm',['$dialog', function($dialog){
    return{
        restrict: 'E',
        transclude: true,
        template: '<span ng-transclude></span>',
        link: function(scope, element, attrs){
            element.css('cursor', 'pointer');
            element.bind('click', function(){
                var message = attrs.message || 'Are you sure?';
                var action = attrs.action;
                var msgbox = $dialog.messageBox(action, message, [{label:'Yes', result: 'yes'},{label:'No', result: 'no'}]);
                scope.$apply(function(){
                    msgbox.open().then(function(result){
                        if(result === 'yes'){
                            if(attrs.onOk) scope.$eval(attrs.onOk);
                        }
                        if(result === 'no'){
                            if(attrs.onCancel) scope.$eval(attrs.onCancel);
                        }
                    });
                });
            });
        },
    }
}]);
