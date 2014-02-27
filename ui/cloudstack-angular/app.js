(function (cloudStack) {
    var app = angular.module('cloudStack', []);

    app.directive('list', function () {
        return {
            restrict: 'E',
            templateUrl: '_list.html',
            replace: true,
            controller: 'ListController'
        };
    });

    app.controller('ListController', function ($scope, $element) {
        var section = $element.attr('path');
        var listView = cloudStack.sections[section].listView;

        $scope.fields = listView.fields;
        $scope.actions = [];
        $scope.data = [];

        // Load data
        listView.dataProvider({
            response: {
                success: function (args) {
                    $(args.data).each(function () {
                        $scope.data.push(this);
                    });
                }
            }
        });

        // Build actions
        for (action in listView.actions) {
            var targetAction = listView.actions[action];

            $scope.actions.push({
                id: targetAction.id,
                label: targetAction.label,
                action: function ($index) {
                    targetAction.action({
                        response: {
                            success: function () {
                                if (targetAction.id == 'remove') {
                                    $scope.data.splice($index, 1);
                                }
                            }
                        }
                    })
                }
            });
        }
    })
}(window.cloudStack));