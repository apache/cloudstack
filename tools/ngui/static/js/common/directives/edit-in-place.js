angular.module('directives.editInPlace', []);
angular.module('directives.editInPlace').directive('editInPlace', function(){
    return {
        restrict: 'E',
        replace: true,
        scope: {
            model: '=',
            attribute: '@',
            onSave: '@'
        },
        templateUrl: '/static/js/common/directives/edit-in-place.tpl.html',
        link: function(scope, element, attrs){
                var modelBackup;
                scope.editing = false;

                scope.edit = function(){
                    scope.editing = true;
                    modelBackup = angular.copy(scope.model);
                }

                scope.save = function(){
                    scope.$eval(attrs.onSave);
                    scope.editing = false;
                }

                scope.cancel = function(){
                    scope.model[scope.attribute] = modelBackup[scope.attribute];
                    scope.editing = false;
                }
        }
    }
});
