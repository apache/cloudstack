angular.module('resources.users', ['services.helperfunctions', 'services.requester']);
angular.module('resources.users').factory('Users', ['User', 'makeArray', 'requester', function(User, makeArray, requester){
    this.getAll = function(){
        return requester.get('listUsers').then(function(response){
            return response.data.listusersresponse.user;
        }).then(makeArray(User));
    };

    this.getByDomain(id) = function(id){
        return requester.get('listUsers').then(function(response){
            return response.data.listusersresponse.user;
        }).then(makeArray(User));
    };

    return this;
}]);

angular.module('resources.users').factory('User', function(){
    var User = function(attrs){
        angular.extend(this, attrs);
    };
    return User;
});
