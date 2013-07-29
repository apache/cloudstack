angular.module('serviceofferings', ['resources.serviceofferings', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/serviceofferings', {
        controller: 'ServiceOfferingsListCtrl',
        templateUrl: 'table.html',
        resolve: {
            serviceofferings: function(ServiceOfferings){
                return ServiceOfferings.fetch();
            }
        }
    })
}]);

angular.module('serviceofferings').controller('ServiceOfferingsListCtrl', ['$scope', 'serviceofferings', 'Breadcrumbs', function($scope, serviceofferings, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('serviceofferings', '/#/serviceofferings');
    $scope.collection = serviceofferings
    $scope.toDisplay = ['name', 'displaytext'];
}]);
