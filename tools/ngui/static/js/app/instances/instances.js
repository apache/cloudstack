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

angular.module("instances", ['resources.virtualmachines', 'services.breadcrumbs', 'services.notifications']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/instances', {
        controller: 'VirtualMachinesListCtrl',
        templateUrl: '/static/js/app/instances/instances.tpl.html',
        resolve:{
            virtualmachines : function(VirtualMachines){
                return VirtualMachines.getAll();
            }
        }
    }).
    when('/instances/:id', {
        controller: 'VirtualMachineDetailCtrl',
        templateUrl: '/static/js/app/instances/instance-details.tpl.html',
        resolve: {
            virtualmachine: function($route, VirtualMachines){
                return VirtualMachines.getById($route.current.params.id);
            }
        }
    })
}]);

angular.module("instances").controller("VirtualMachinesListCtrl", 
        ["$scope", "virtualmachines", "Breadcrumbs", "Notifications", function($scope, virtualmachines, Breadcrumbs, Notifications){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Instances', '/#/instances');
    $scope.collection = virtualmachines;
    $scope.toDisplay = ["displayname", "instancename", "zonename", "state"];
}]);

angular.module("instances").controller("VirtualMachineDetailCtrl", ["$scope", "virtualmachine", "Breadcrumbs", function($scope, virtualmachine, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Instances', '/#/instances');
    Breadcrumbs.push(virtualmachine.displayname, '/#/instances/'+ virtualmachine.id);
    $scope.model = virtualmachine;
}]);
