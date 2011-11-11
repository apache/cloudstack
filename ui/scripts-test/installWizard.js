(function($, cloudStack, testData) {
  cloudStack.installWizard = {
    // Check if install wizard should be invoked
    check: function(args) {
      args.response.success({
        doInstall: args.context.users[0].username == 'newuser'
      });
    },

    changeUser: function(args) {
      setTimeout(function() {
        args.response.success({
          data: {
            newUser: {
              username: args.data.username
            }
          }
        });
      }, 500);
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

      var createZone = function(args) {
        createPod();
      };

      var createPod = function(args) {
        createIPRange();
      };

      var createIPRange = function(args) {
        createCluster();
      };

      var createCluster = function(args) {
        createHost();
      };

      var createHost = function(args) {
        createPrimaryStorage();
      };

      var createPrimaryStorage = function(args) {
        createSecondaryStorage();
      };

      var createSecondaryStorage = function(args) {
        pollSystemVMs();
      };

      var pollSystemVMs = function() {
        setTimeout(function() {
          complete();          
        }, 5000);
      };

      createZone();
    }
  };
}(jQuery, cloudStack, testData));
