angular.module('directives.label', []);
angular.module('directives.label').directive('vmStateLabel', function(){
    return {
        restrict: 'E',
        replace: true,
        scope: {
            vm: '=',
        },
        template : '<span ng-class="class">{{vm.state}}</span>',
        link: function(scope, element, attrs){
            var setClass = function(){
                if(scope.vm.state === "Running") scope.class="label label-success";
                else if (scope.vm.state === "Stopped") scope.class="label label-important";
                else if(scope.vm.state === "Destroyed") scope.class="label label-inverse";
                else scope.class="label label-info";
            }

            setClass();

            scope.$watch('vm', function(){
                setClass();
            }, true);
        }
    }
})
