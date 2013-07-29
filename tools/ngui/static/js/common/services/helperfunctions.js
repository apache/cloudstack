angular.module('services.helperfunctions', []);
angular.module('services.helperfunctions').factory('makeArray', function(){
    var makeArray = function(Type){
        return function(response){
            var collection = [];
            angular.forEach(response, function(data){
                collection.push(new Type(data));
            });
            return collection;
        }
    }
    return makeArray;
});

angular.module('services.helperfunctions').factory('makeInstance', function(){
    var makeInstance = function(Type){
        return function(response){
            return new Type(response);
        }
    }
    return makeInstance;
});
