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

angular.module('networks', ['resources.networks', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/networks',{
        controller: 'NetworksListCtrl',
        templateUrl: 'table.html',
        resolve: {
            networks: function(Networks){
                return Networks.fetch();
            }
        }
    })
}]);

angular.module('networks').controller('NetworksListCtrl', ['$scope', 'networks', 'Breadcrumbs', function($scope, networks, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('networks', '/#/networks');
    $scope.collection = networks;
    $scope.toDisplay = ['name', 'type', 'zonename'];
}]);
