angular.module('resources.zones', ['services.helperfunctions', 'services.requester']);
angular.module('resources.zones').factory('Zones', ['Zone', 'makeArray', 'requester', function(Zone, makeArray, requester){
    this.getAll = function(){
        return requester.get('listZones').then(function(response){
            return response.data.listzonesresponse.zone;
        }).then(makeArray(Zone));
    };
    return this;
}]);

angular.module('resources.zones').factory('Zone', function(){
    var Zone = function(attrs){
        angular.extend(this, attrs);
    };
    return Zone;
});
