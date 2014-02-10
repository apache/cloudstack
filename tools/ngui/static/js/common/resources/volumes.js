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

angular.module('resources.volumes', ['services.helperfunctions', 'services.requester']);
angular.module('resources.volumes').factory('Volumes', ['$http', 'Volume', 'makeArray', 'requester', function($http, Volume, makeArray, requester){
    this.getAll = function(){
        return requester.get('listVolumes').then(function(response){
            return response.data.listvolumesresponse.volume;
        }).then(makeArray(Volume));
    };
    return this;
}]);

angular.module('resources.volumes').factory('Volume', function(){
    var Volume = function(attrs){
        angular.extend(this, attrs);
    }
    return Volume;
});
