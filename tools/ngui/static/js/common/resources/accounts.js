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
