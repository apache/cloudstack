angular.module('resources.domains', ['services.helperfunctions', 'services.requester']);
angular.module('resources.domains').factory('Domains', ['$http', 'Domain', 'makeArray', 'requester', function($http, Domain, makeArray, requester){
    this.fetch = function(){
        return requester.get('listDomains').then(function(response){
            return response.data.listdomainsresponse.domain;
        }).then(makeArray(Domain));
    };
    return this;
}]);

angular.module('resources.domains').factory('Domain', function(){
    var Domain = function(attrs){
        angular.extend(this, attrs);
    }
    return Domain;
});
