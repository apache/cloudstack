angular.module('events', ['resources.events', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/events', {
        controller: 'EventsListCtrl',
        templateUrl: 'table.html',
        resolve: {
            events: function(Events){
                return Events.fetch();
            }
        }
    })
}]);

angular.module('events').controller('EventsListCtrl', ['$scope', 'events', 'Breadcrumbs', function($scope, events, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('events', '/#/events');
    $scope.collection = events;
    $scope.toDisplay = ['type', 'description', 'account', 'created'];
}]);
