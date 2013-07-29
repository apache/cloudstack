angular.module('resources.virtualmachines',['services.helperfunctions', 'services.requester']);
angular.module('resources.virtualmachines').factory('VirtualMachines',
        ['$http', 'VirtualMachine', 'makeArray', 'makeInstance', 'requester', function($http, VirtualMachine, makeArray, makeInstance, requester){
    this.getAll = function(){
        return requester.get('listVirtualMachines').then(function(response){
            return response.data.listvirtualmachinesresponse.virtualmachine;
        }).then(makeArray(VirtualMachine));
    };

    this.getById = function(id){
        return requester.get('listVirtualMachines', {id: id}).then(function(response){
            return response.data.listvirtualmachinesresponse.virtualmachine[0];
        }).then(makeInstance(VirtualMachine));
    };

    return this;
}]);

angular.module('resources.virtualmachines').factory('VirtualMachine', ['requester', function (requester){
    var VirtualMachine = function(attrs){
        angular.extend(this, attrs);
    };
    VirtualMachine.prototype.start = function(){
        var self = this;
        self.state = 'Starting';
        requester.async('startVirtualMachine', {id : self.id}).then(function(response){
            self.state = 'Running';
        });
    };
    VirtualMachine.prototype.stop = function(){
        var self = this;
        self.state = 'Stopping'
        requester.async('stopVirtualMachine', {id : self.id}).then(function(response){
            self.state = 'Stopped';
        });
    };
    VirtualMachine.prototype.reboot = function(){
        var self = this;
        self.state = 'Rebooting';
        requester.async('rebootVirtualMachine', {id: self.id}).then(function(response){
            self.state = 'Running';
        });
    };
    VirtualMachine.prototype.destroy = function(){
        var self = this;
        requester.async('destroyVirtualMachine', {id: self.id}).then(function(response){
            self.state = 'Destroyed';
        });
    };
    VirtualMachine.prototype.restore = function(){
        var self = this;
        self.state = "Restoring";
        requester.async('restoreVirtualMachine', {id: self.id}).then(function(response){
            self.state = "Stopped";
        });
    };
    return VirtualMachine;
}]);
