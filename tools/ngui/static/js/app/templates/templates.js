angular.module('templates', ['resources.templates', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/templates', {
        controller: 'TemplatesListCtrl',
        templateUrl: 'table.html',
        resolve: {
            templates: function(Templates){
                return Templates.getAll();
            }
        }
    })
}]);

angular.module('templates').controller('TemplatesListCtrl', ['$scope', 'templates', 'Breadcrumbs', function($scope, templates, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Templates', '/#/templates');
    $scope.collection = templates;
    $scope.toDisplay = ['name', 'domain', 'hypervisor'];
}]);
