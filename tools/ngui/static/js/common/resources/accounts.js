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

angular.module('resources.accounts', ['services.helperfunctions', 'services.requester']);
angular.module('resources.accounts').factory('Accounts', ['Account', 'requester', 'makeArray', 'makeInstance', function(Account, requester, makeArray, makeInstance){
    var Accounts = {};

    Accounts.getAll = function(){
        return requester.get('listAccounts').then(function(response){
            return response.data.listaccountsresponse.account;
        }).then(makeArray(Account));
    };

    Accounts.create = function(details){
        return requester.get('createAccount', details).then(function(response){
            return response.data.createaccountresponse.account;
        }).then(makeInstance(Account));
    }
    return Accounts;
}]);

angular.module('resources.accounts').factory('Account', function(){
    var Account = function(attrs){
        angular.extend(this, attrs);
    };
    return Account;
});
