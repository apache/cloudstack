(function($, cloudStack, testData) {
  cloudStack.installWizard = {
    // Check if install wizard should be invoked
    check: function(args) {
      args.response.success({
        doInstall: false
      });
      
      // $.ajax({
      //   url: createURL('listZones'),
      //   dataType: 'json',
      //   async: true,
      //   success: function(data) {
      //     args.response.success({
      //       doInstall: !data.listzonesresponse.zone
      //     });
      //   }
      // });
    },

    changeUser: function(args) {
      $.ajax({
        url: createURL('updateUser'),
        data: {
          id: cloudStack.context.users[0].userid,
          password: md5Hashed ? $.md5(args.data.password) : args.data.password
        },
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            data: { newUser: data.updateuserresponse.user }
          });
        }
      });
    },

    // Copy text
    copy: {
      whatIsCloudStack: function(args) {
        args.response.success({
          text: 'CloudStack is open source software written in java that is designed to deploy and manage large networks of virtual machines, as a highly available, scalable cloud computing platform. CloudStack current supports the most popular open source hypervisors VMware, Oracle VM, KVM, XenServer and Xen Cloud Platform. CloudStack offers three ways to manage cloud computing environments: a easy-to-use web interface, command line and a full-featured RESTful API.'
        });
      },

      whatIsAZone: function(args) {
        args.response.success({
          text: 'A zone is integral to the CloudStack platform -- your entire network is represented via a zone. More text goes here...'
        });
      },

      whatIsAPod: function(args) {
        args.response.success({
          text: 'A pod is a part of a zone. More text goes here...'
        });
      },

      whatIsACluster: function(args) {
        args.response.success({
          text: 'A cluster is a part of a zone. More text goes here...'
        });
      },

      whatIsAHost: function(args) {
        args.response.success({
          text: 'A host is a part of a zone. More text goes here...'
        });
      },

      whatIsPrimaryStorage: function(args) {
        args.response.success({
          text: 'Primary storage is a part of a zone. More text goes here...'
        });
      },

      whatIsSecondaryStorage: function(args) {
        args.response.success({
          text: 'Secondary storage is a part of a zone. More text goes here...'
        });
      }
    },

    action: function(args) {
      var complete = args.response.success;
      var data = args.data

      /**
       * Step 1: add zone
       */
      var createZone = function(args) {
        debugger;
        $.ajax({
          url: createURL('createZone'),
          data: {
            name: data.zone.name,
            networktype: 'Basic',
            internaldns1: data.zone.internaldns1,
            internaldns2: data.zone.internaldns2
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

      /**
       * Step 2: add pod
       */
      var createPod = function(args) {
        $.ajax({
          url: createURL('createPod'),
          data: {
            name: data['pod-name'],
            zoneid: args.data.zone.id,
            gateway: data['pod-gateway'],
            netmask: data['pod-netmask'],
            startip: data['pod-ip-range-start'],
            endip: data['pod-ip-range-end']
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

      /**
       * Step 3: add public IP range
       */
      var createIPRange = function(args) {
        $.ajax({
          url: createURL('createVlanIpRange'),
          data: {
            zoneid: args.data.zone.id,
            vlan: 'untagged',
            gateway: data['guest-gateway'],
            netmask: data['guest-netmask'],
            startip: data['guest-ip-range-start'],
            endip: data['guest-ip-range-end']
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

      /**
       * Step 4: add cluster
       */
      var createCluster = function(args) {
        $.ajax({
          url: createURL('addCluster'),
          data: {
            clustername: data.cluster.name,
            podid: args.data.pod.id,
            zoneid: args.data.zone.id,
            hypervisor: data.cluster.hypervisor,
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

      /**
       * Step 5: add host
       */
      var createHost = function(args) {
        $.ajax({
          url: createURL('addHost'),
          data: {
            clustername: args.data.cluster.name,
            zoneid: args.data.zone.id,
            podid: args.data.pod.id,
            hypervisor: 'XenServer',
            clustertype: 'CloudManaged',
            url: 'http://' + data.host.hostname,
            username: data.host.username,
            password: data.host.password
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

      /**
       * Step 6: add primary storage
       */
      var createPrimaryStorage = function(args) {
        $.ajax({
          url: createURL('createStoragePool'),
          data: {
            name: data.primaryStorage.name,
            clusterid: args.data.cluster.id,
            zoneid: args.data.zone.id,
            podid: args.data.pod.id,
            hypervisor: 'XenServer',
            clustertype: 'CloudManaged',
            url: 'nfs://' + data.primaryStorage.server + data.primaryStorage.path
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

      /**
       * Step 7: add secondary storage
       */
      var createSecondaryStorage = function(args) {
        $.ajax({
          url: createURL('addSecondaryStorage'),
          data: {
            clusterid: args.data.cluster.id,
            zoneid: args.data.zone.id,
            url: 'nfs://' + data.secondaryStorage.nfsServer + data.secondaryStorage.path
          },
          dataType: 'json',
          async: true,
          success: function(data) {
            pollSystemVMs();
          }
        });
      };

      /**
       * Final step: poll for system VMs, wait until they are active to complete wizard
       */
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
