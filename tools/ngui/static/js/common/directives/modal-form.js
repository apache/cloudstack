// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

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
