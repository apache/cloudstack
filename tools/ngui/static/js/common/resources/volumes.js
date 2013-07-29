angular.module('resources.volumes', ['services.helperfunctions', 'services.requester']);
angular.module('resources.volumes').factory('Volumes', ['$http', 'Volume', 'makeArray', 'requester', function($http, Volume, makeArray, requester){
    this.getAll = function(){
        return requester.get('listVolumes').then(function(response){
            return response.data.listvolumesresponse.volume;
        }).then(makeArray(Volume));
    };
    return this;
}]);

angular.module('resources.volumes').factory('Volume', function(){
    var Volume = function(attrs){
        angular.extend(this, attrs);
    }
    return Volume;
});
