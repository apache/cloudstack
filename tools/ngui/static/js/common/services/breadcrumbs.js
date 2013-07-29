angular.module('services.breadcrumbs', []);
angular.module('services.breadcrumbs').factory('Breadcrumbs', ['$rootScope', '$location', function($rootScope, $location){
    var breadcrumbs = [{id:'home', url:'/#/'}];
    var Breadcrumbs = {};
    Breadcrumbs.refresh = function(){
        breadcrumbs = [{name:'Home', url:'/#/'}];
    };
    Breadcrumbs.push = function(name, url){
        breadcrumbs.push({name: name, url: url})
    };
    Breadcrumbs.getAll = function(){
        return breadcrumbs;
    };
    return Breadcrumbs;
}]);
