angular.module('domains', ['resources.domains', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/domains',{
        controller: 'DomainsListCtrl',
        templateUrl: 'table.html',
        resolve: {
            domains: function(Domains){
                return Domains.fetch();
            }
        }
    })
}]);

var DomainsListCtrl = angular.module('domains').controller('DomainsListCtrl', ['$scope', 'domains', 'Breadcrumbs', function($scope, domains, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('domains', '/#/domains');
    $scope.collection = domains;
    $scope.toDisplay = ['id', 'name'];
}]);
