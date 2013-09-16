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
