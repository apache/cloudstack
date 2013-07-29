angular.module('resources.configurations', ['services.helperfunctions', 'services.requester', 'services.notifications']);
angular.module('resources.configurations').factory('Configurations', ['$http', 'Configuration', 'makeArray', 'requester', function($http, Configuration, makeArray, requester){
    var Configurations = {};

    Configurations.getAll = function(){
        return requester.get('listConfigurations').then(function(response){
            return response.data.listconfigurationsresponse.configuration;
        }).then(makeArray(Configuration));
    }

    return Configurations;
}]);

angular.module('resources.configurations').factory('Configuration', ['requester', 'Notifications', function(requester, Notifications){
    var Configuration = function(attrs){
        angular.extend(this, attrs);
    }

    Configuration.prototype.update = function(){
        return requester.get('updateConfiguration', {name: this.name, value: this.value}).then(function(response){
            return response.data.updateconfigurationresponse.configuration;
        }).then(function(response){
            Notifications.push('success', 'Updated ' + response.name + '. Please restart management server(s) for new settings to take effect');
        });
    };
    return Configuration;
}]);
