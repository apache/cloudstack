angular.module('resources.events', ['services.helperfunctions', 'services.requester']);
angular.module('resources.events').factory('Events', ['$http', 'Event', 'makeArray', 'requester', function($http, Event, makeArray, requester){
    this.fetch = function(){
        return requester.get('listEvents').then(function(response){
            return response.data.listeventsresponse.event;
        }).then(makeArray(Event));
    }
    return this;
}]);

angular.module('resources.events').factory('Event', function(){
    var Event = function(attrs){
        angular.extend(this, attrs);
    }
    return Event;
});
