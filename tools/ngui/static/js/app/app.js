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

angular.module('cloudstack', [
        'ui.bootstrap',
        'instances',
        'storage',
        'networks',
        'templates',
        'events',
        'accounts',
        'domains',
        'projects',
        'globalsettings', 
        'serviceofferings',
        'services.breadcrumbs',
        'services.notifications',
        'directives.confirm',
        'directives.modalForm',
        'directives.label',
        'directives.editInPlace',
        ]).
config(["$routeProvider", function($routeProvider){
    $routeProvider.
    when('/',{
        controller: "DefaultCtrl",
        templateUrl: "default.html"
    }).
    otherwise({
        redirectTo: '/'
    })
}]);

angular.module("cloudstack").controller("DefaultCtrl", ["$scope", "Breadcrumbs", function($scope, Breadcrumbs){
    Breadcrumbs.refresh();
}]);

angular.module("cloudstack").controller("AppCtrl", ["$scope", "Breadcrumbs", "Notifications", "Dictionary", "$rootScope", 
        function($scope, Breadcrumbs, Notifications, Dictionary, $rootScope){
    $scope.breadcrumbs = Breadcrumbs;
    $scope.dictionary = Dictionary;
    $scope.notifications = Notifications;

    $scope.loading = false;

    $rootScope.$on("$routeChangeStart", function(event, next, current){
        $scope.loading = true;
    });

    $rootScope.$on("$routeChangeSuccess", function(event, current, previous){
        $scope.loading = false;
    });
}]);

angular.module("cloudstack").controller("HeaderCtrl", ["$scope", function($scope){
}]);

angular.module("cloudstack").controller("NavCtrl", ["$scope", "$location", function($scope, $location){
    $scope.isActive = function(page){
        if($location.path() === '/' && page === '/') return 'active'; //home page
        return $location.path().split('/')[1] === page? 'active': '';
    }
}]);
