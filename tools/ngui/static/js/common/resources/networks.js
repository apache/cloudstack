angular.module('resources.networks',['services.helperfunctions', 'services.requester']);
angular.module('resources.networks').factory('Networks', ['$http', 'Network', 'makeArray', 'requester', function($http, Network, makeArray, requester){
    this.fetch = function(){
        return requester.get('listNetworks').then(function(response){
            return response.data.listnetworksresponse.network;
        }).then(makeArray(Network));
    };
    return this;
}]);

angular.module('resources.networks').factory('Network', function(){
    var Network = function(attrs){
        angular.extend(this, attrs);
    };
    return Network;
});
