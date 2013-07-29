angular.module('resources.diskofferings', ['services.helperfunctions', 'services.requester']);
angular.module('resources.diskofferings').factory('DiskOfferings', ['DiskOffering', 'makeArray', 'requester', function(DiskOffering, makeArray, requester){
    this.getAll = function(){
        return requester.get('listDiskOfferings').then(function(response){
            return response.data.listdiskofferingsresponse.diskoffering
        }).then(makeArray(DiskOffering));
    };
    return this;
}]);

angular.module('resources.diskofferings').factory('DiskOffering', function(){
    var DiskOffering = function(attrs){
        angular.extend(this, attrs);
    };
    return DiskOffering;
});
