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
