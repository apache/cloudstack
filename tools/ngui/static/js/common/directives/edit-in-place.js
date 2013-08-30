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
