angular.module('projects', ['resources.projects', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/projects', {
        controller: 'ProjectsListCtrl',
        templateUrl: 'table.html',
        resolve: {
            projects: function(Projects){
                return Projects.fetch();
            }
        }
    })
}]);

angular.module('projects').controller('ProjectsListCtrl', ['$scope', 'projects', 'Breadcrumbs', function($scope, projects, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('projects', '/#/projects');
    $scope.collection = projects;
    $scope.toDisplay = ['name', 'displaytext', 'domain', 'account', 'state']
}]);
