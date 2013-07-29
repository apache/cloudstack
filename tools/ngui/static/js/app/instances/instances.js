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
