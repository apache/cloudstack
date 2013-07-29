angular.module('resources.projects', ['services.helperfunctions', 'services.requester']);
angular.module('resources.projects').factory('Projects', ['Project', 'makeArray', 'requester', function(Project, makeArray, requester){
    this.fetch = function(){
        return requester.get('listProjects').then(function(response){
            return response.data.listprojectsresponse.project;
        }).then(makeArray(Project));
    };
    return this;
}]);

angular.module('resources.projects').factory('Project', function(){
    var Project = function(attrs){
        angular.extend(this, attrs);
    };
    return Project;
});
