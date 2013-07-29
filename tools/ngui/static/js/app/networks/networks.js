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
