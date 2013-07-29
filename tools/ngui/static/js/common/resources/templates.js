angular.module('resources.templates', ['services.helperfunctions', 'services.requester']);
angular.module('resources.templates').factory('Templates', ['Template', 'makeArray', 'requester', function(Template, makeArray, requester){
    this.getAll = function(){
        return requester.get('listTemplates', {templatefilter: 'all'}).then(function(response){
            return response.data.listtemplatesresponse.template;
        }).then(makeArray(Template));
    };
    return this;
}]);

angular.module('resources.templates').factory('Template', function(){
    var Template = function(attrs){
        angular.extend(this, attrs);
    };
    return Template;
});
