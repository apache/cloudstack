angular.module('directives.modalForm', ['ui.bootstrap.dialog']);
angular.module('directives.modalForm').directive('modalForm', ['$dialog', function($dialog){
    return {
        restrict: 'EA',
        transclude: true,
        template: '<span ng-transclude></span>',
        scope: {
            onSubmit: '&',
            template: '@',
            formDetails: '='
        },
        link: function(scope, element, attrs){
            var opts = {
                backdrop: true,
                backdropClick: true,
                backdropFade: true,
                templateUrl: '/static/js/common/directives/modal-form.tpl.html',
                resolve: {
                    formDetails: function(){
                        return scope.formDetails;
                    }
                },
                controller: 'FormCtrl',
            }
            element.bind('click', function(){
                var formDialog = $dialog.dialog(opts);
                var dialogPromise;
                scope.$apply(function(){
                    dialogPromise = formDialog.open()
                });
                dialogPromise.then(function(result){
                    if(result) scope.formDetails.onSubmit(result);
                });
            });
        }
    }
}]);

angular.module('directives.modalForm').controller('FormCtrl', ['$scope', 'dialog', 'formDetails', 'Dictionary',
        function TestDialogController($scope, dialog, formDetails, Dictionary){
    $scope.dictionary = Dictionary;
    //formObject will be passed into onSubmit when submit is clicked
    $scope.formObject = {};
    $scope.template = 'table.html';
    $scope.formDetails = formDetails;
    $scope.title = formDetails.title;
    $scope.close = function(){
        dialog.close();
    };
    $scope.submit = function(){
        dialog.close($scope.formObject);
    };
}]);
