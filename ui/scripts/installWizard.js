(function($, cloudStack, testData) {
  cloudStack.installWizard = {
    // Check if install wizard should be invoked
    check: function(args) {
      $.ajax({
        url: createURL('listZones'),
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            doInstall: !data.listzonesresponse.zone
          });
        }
      });
    },

    action: function(args) {
      var complete = args.response.success;

      var createZone = function(args) {
        $.ajax({
          url: createURL('createZone'),
          data: {
            name: 'brian-zone',
            networktype: 'Basic',
            dns1: '8.8.8.8',
            internaldns1: '10.223.110.223'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            createPod({
              data: {
                zone: data.createzoneresponse.zone
              }
            });
          }
        });
      };

      var createPod = function(args) {
        $.ajax({
          url: createURL('createPod'),
          data: {
            name: 'brian-pod',
            zoneid: args.data.zone.id,
            gateway: '10.223.183.1',
            netmask: '255.255.255.0',
            startip: '10.223.183.10',
            endip: '10.223.183.20'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            createIPRange({
              data: $.extend(args.data, {
                pod: data.createpodresponse.pod
              })
            });
          }
        });
      };

      var createIPRange = function(args) {
        $.ajax({
          url: createURL('createVlanIpRange'),
          data: {
            name: 'brian-zone',
            zoneid: args.data.zone.id,
            vlan: 'untagged',
            gateway: '10.223.183.1',
            netmask: '255.255.255.0',
            startip: '10.223.183.50',
            endip: '10.223.183.100'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            createCluster({
              data: $.extend(args.data, {
                ipRange: data.createvlaniprangeresponse.vlan
              })
            });
          }
        });
      };

      var createCluster = function(args) {
        $.ajax({
          url: createURL('addCluster'),
          data: {
            clustername: 'brian-cluster-xen',
            podid: args.data.pod.id,
            zoneid: args.data.zone.id,
            hypervisor: 'XenServer',
            clustertype: 'CloudManaged'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            createHost({
              data: $.extend(args.data, {
                cluster: data.addclusterresponse.cluster[0]
              })
            });
          }
        });
      };

      var createHost = function(args) {
        $.ajax({
          url: createURL('addHost'),
          data: {
            clustername: 'brian-cluster-xen',
            zoneid: args.data.zone.id,
            podid: args.data.pod.id,
            hypervisor: 'XenServer',
            clustertype: 'CloudManaged',
            url: 'http://10.223.183.2',
            username: 'root',
            password: 'password'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            createPrimaryStorage({
              data: $.extend(args.data, {
                host: data.addhostresponse.host[0]
              })
            });
          }
        });
      };

      var createPrimaryStorage = function(args) {
        $.ajax({
          url: createURL('createStoragePool'),
          data: {
            name: 'brian-primary-storage',
            clusterid: args.data.cluster.id,
            zoneid: args.data.zone.id,
            podid: args.data.pod.id,
            hypervisor: 'XenServer',
            clustertype: 'CloudManaged',
            url: 'nfs://10.223.110.232/export/home/bfederle/primary'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            createSecondaryStorage({
              data: $.extend(args.data, {
                host: data.createstoragepoolresponse.storagepool
              })
            });
          }
        });
      };

      var createSecondaryStorage = function(args) {
        $.ajax({
          url: createURL('addSecondaryStorage'),
          data: {
            clusterid: args.data.cluster.id,
            zoneid: args.data.zone.id,
            url: 'nfs://10.223.110.232/export/home/bfederle/secondary'
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            pollSystemVMs();
          }
        });
      };

      var pollSystemVMs = function() {
        var poll = setInterval(function() {
          $.ajax({
            url: createURL('listSystemVms'),
            dataType: 'json',
            async: true,
            success: function(data) {
              if (data.listsystemvmsresponse.systemvm) {
                clearInterval(poll);
                complete();
              }
            }
          });
        }, 1000);
      };

      createZone();
    }
  };
}(jQuery, cloudStack, testData));
