angular.module('resources.snapshots', ['services.helperfunctions', 'services.requester']);
angular.module('resources.snapshots').factory('Snapshots', ['Snapshot', 'makeArray', 'requester', function(Snapshot, makeArray, requester){
    this.getAll = function(){
        return requester.get('listSnapshots').then(function(response){
            return response.data.listsnapshotsresponse.snapshot;
        }).then(makeArray(Snapshot));
    };
    return this;
}]);

angular.module('resources.snapshots').factory('Snapshot', function(){
    var Snapshot = function(attrs){
        angular.extend(this, attrs);
    };
    return Snapshot;
});
