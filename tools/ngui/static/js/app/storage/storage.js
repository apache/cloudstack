angular.module("storage", ["resources.volumes", "resources.snapshots", "resources.zones", "resources.diskofferings", "services.breadcrumbs"]).
config(['$routeProvider', function($routeProvider){
    $routeProvider.
    when('/volumes',{
        controller: 'VolumesListCtrl',
        templateUrl: '/static/js/app/storage/storage.tpl.html',
        resolve: {
            volumes: function(Volumes){
                return Volumes.getAll();
            }
        }
    }).
    when('/snapshots', {
        controller: 'SnapshotsListCtrl',
        templateUrl: 'table.html',
        resolve:{
            snapshots: function(Snapshots){
                return Snapshots.getAll();
            }
        }
    })
}]);

angular.module("storage").controller("VolumesListCtrl", ["$scope", "$location", "volumes", "Breadcrumbs", "Volumes", "Zones", "DiskOfferings",
        function($scope, $location, volumes, Breadcrumbs, Volumes, Zones, DiskOfferings){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Volumes', '/#/volumes');
    $scope.collection = volumes;
    $scope.view = 'volumes';
    $scope.toDisplay = ['name', 'type', 'hypervisor', 'vmdisplayname'];

    $scope.addVolumeForm = {
        title: 'Add Volume',
        onSubmit: Volumes.getAll,
        fields: [
            {
                model: 'name',
                type: 'input-text',
                label: 'name',
                required: true
            },
            {
                model: 'zoneid',
                type: 'select',
                label: 'availabilityZone',
                options: Zones.getAll,
                getValue: function(model){
                    return model.id;
                },
                getName: function(model){
                    return model.name;
                }
            },
            {
                model: 'diskofferingid',
                type: 'select',
                label: 'diskoffering',
                options: DiskOfferings.getAll,
                getValue: function(model){
                    return model.id;
                },
                getName: function(model){
                    return model.name;
                }
            }
        ]
    };

    $scope.uploadVolumeForm = {
        title: 'Upload Volume',
        onSubmit: Volumes.getAll,
        fields: [
            {
                model: 'name',
                type: 'input-text',
                label: 'name',
            },
            {
                model: 'zoneid',
                type: 'select',
                label: 'availabilityZone',
                options: Zones.getAll,
                getValue: function(model){
                    return model.id;
                },
                getName: function(model){
                    return model.name;
                }
            },
            {
                model: 'format',
                type: 'select',
                label: 'format',
                options: function(){
                    return ['RAW', 'VHD', 'OVA', 'QCOW2'];
                },
                getValue: function(model){
                    return model;
                },
                getName: function(model){
                    return model;
                }
            },
            {
                model: 'url',
                type: 'input-text',
                label: 'url'
            },
            {
                model: 'checksum',
                type: 'input-text',
                label: 'checksum'
            }
        ],
    }

    $scope.$watch('view', function(newVal, oldVal){
        if(newVal === oldVal) return;
        if(newVal === 'volumes') return;
        else $location.path('/snapshots');
    });
}]);

angular.module("storage").controller("SnapshotsListCtrl", ["$scope", "$location", "snapshots", "Breadcrumbs", function($scope, $location, snapshots, Breadcrumbs){
    Breadcrumbs.refresh();
    Breadcrumbs.push('Snapshots', '/#/snapshots');
    $scope.collection = snapshots;
    $scope.view = "snapshots";
    $scope.toDisplay = ['volumename', 'intervaltype', 'created', 'state'];

    $scope.$watch('view', function(newVal, oldVal){
        if(newVal === oldVal) return;
        if(newVal === 'snapshots') return;
        else $location.path('/volumes');
    });
}]);
