angular.module('resources.serviceofferings', ['services.helperfunctions', 'services.requester']);
angular.module('resources.serviceofferings').factory('ServiceOfferings', ['$http', 'ServiceOffering', 'makeArray', 'requester', function($http, ServiceOffering, makeArray, requester){
    this.fetch = function(){
        return requester.get('listServiceOfferings').then(function(response){
            return response.data.listserviceofferingsresponse.serviceoffering;
        }).then(makeArray(ServiceOffering));
    };
    return this;
}]);

angular.module('resources.serviceofferings').factory('ServiceOffering', function(){
    var ServiceOffering = function(attrs){
        angular.extend(this, attrs);
    }
    return ServiceOffering;
});
