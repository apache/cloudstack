angular.module('globalsettings', ['resources.configurations', 'services.breadcrumbs', 'services.notifications']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/configurations', {
        controller: 'ConfigurationsListCtrl',
        templateUrl: '/static/js/app/globalsettings/globalsettings.tpl.html',
        resolve: {
            configurations: function(Configurations){
                return Configurations.getAll();
            }
        }
    })
}]);

angular.module('globalsettings').controller('ConfigurationsListCtrl', ['$scope', 'configurations', 'Breadcrumbs', 'Notifications', 
        function($scope, configurations, Breadcrumbs, Notifications){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Configurations', '/#/configurations');
    $scope.collection = configurations;
    $scope.toDisplay = ['name', 'description', 'value'];
}]);
