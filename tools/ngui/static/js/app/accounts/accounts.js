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

angular.module('accounts', ['resources.accounts', 'resources.domains', 'services.breadcrumbs']).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/accounts', {
        controller: 'AccountsListCtrl',
        templateUrl: '/static/js/app/accounts/accounts.tpl.html',
        resolve: {
            accounts: function(Accounts){
                return Accounts.getAll();
            }
        }
    })
}]);

angular.module('accounts').controller('AccountsListCtrl', ['$scope', 'accounts', 'Breadcrumbs', 'Accounts', 'Domains',
        function($scope, accounts, Breadcrumbs, Accounts, Domains){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Accounts', '/#/accounts');
    $scope.collection = accounts;
    $scope.toDisplay = ['name', 'domain', 'state'];

    $scope.addAccountForm = {
        title: 'Add Account',
        onSubmit: Accounts.create,
        fields: [
            {
                model: 'username',
                type: 'input-text',
                label: 'username'
            },
            {
                model: 'password',
                type: 'input-password',
                label: 'password'
            },
            {
                model: 'email',
                type: 'input-text',
                label: 'email'
            },
            {
                model: 'firstname',
                type: 'input-text',
                label: 'firstname'
            },
            {
                model: 'lastname',
                type: 'input-text',
                label: 'lastname'
            },
            {
                model: 'domainid',
                type: 'select',
                label: 'domain',
                options: Domains.fetch,
                getName: function(model){
                    return model.name;
                },
                getValue: function(model){
                    return model.id;
                }
            },
            {
                model: 'account',
                type: 'input-text',
                label: 'account'
            },
            {
                model: 'accounttype',
                type: 'select',
                label: 'type',
                options: function(){
                    return ['User', 'Admin']
                },
                getName: function(model){
                    return model;
                },
                getValue: function(model){
                    //return 0 if user, else 1
                    return model === 'User'?0:1;
                }
            }
        ]
    }
}]);
