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

angular.module('services.notifications', []);
angular.module('services.notifications').factory('Notifications', function(){
    var notifications = [];
    var Notifications = {};
    Notifications.push = function(type, msg){
        notifications.push({type: type, msg: msg});
    };
    Notifications.getAll = function(){
        return notifications;
    };
    Notifications.remove = function(notification){
        var index = notifications.indexOf(notification);
        notifications.splice(index, 1);//remove element from the array, ugly
    };

    return Notifications;
});
