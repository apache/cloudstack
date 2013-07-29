angular.module('services.requester', [])
angular.module('services.requester').factory('requester', ['$http', '$timeout', '$q', function($http, $timeout, $q){
    var baseURL = '/api/'; //make a provider
    var requester = {};
    requester.get = function(command, params){
        return $http.get(baseURL + command, {params: params});
    };
    requester.async = function(command, params){
        var deferred = $q.defer();
        $http.get(baseURL + command, {params : params}).then(function(response){
            var responseName = command.toLowerCase() + 'response';
            var jobId = response.data[responseName]['jobid'];
            var poll = function(){
                $timeout(function(){
                    $http.get(baseURL + 'queryAsyncJobResult', {params : {jobId: jobId}}).then(function(response){
                        if(response.data.queryasyncjobresultresponse.jobstatus){
                            deferred.resolve(response.data.queryasyncjobresultresponse.jobresult);
                        }
                        else{
                            poll();
                        }
                    })
                }, 5000, false);
            };
            poll();
        })
        return deferred.promise;
    };
    return requester;
}]);
