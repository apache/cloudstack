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
