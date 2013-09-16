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
