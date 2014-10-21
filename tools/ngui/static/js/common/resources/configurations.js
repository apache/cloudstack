// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

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
