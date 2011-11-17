(function(cloudStack) {
  cloudStack.sections.domains = {
    title: 'Domains',
    id: 'domains',

    // Domain tree
    treeView: {
      // Details
      detailView: {
        name: 'Domain details',
        viewAll: {
          label: 'Accounts',
          path: 'accounts.accounts'
        },

        // Detail actions
        actions: {
          // Destroy
          destroy: {
            label: 'Remove domain',
            messages: {
              confirm: function(args) {
                return 'Are you sure you want to destroy this domain?';
              },
              notification: function(args) {
                return 'Removed domain: ' + args.name;
              }
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success();
              }, 200);
            },
            notification: {
              poll: testData.notifications.testPoll
            }
          },
          
          // Edit domain
          edit: {
            label: 'Edit domain details',
            messages: {
              notification: function(args) {
                return 'Edited domain: ' + args.name;
              }
            },
            action: function(args) {
              setTimeout(function() {
                args.response.success();
              }, 200);
            }
          },

          // Add domain
          create: {
            label: 'Add domain',

            action: function(args) {
              args.response.success();
            },

            messages: {
              notification: function(args) {
                return 'Created domain';
              }
            },

            createForm: {
              title: 'Add subdomain',
              desc: 'Please specify the domain you want to create.',
              fields: {
                name: {
                  label: 'Name',
                  validation: { required: true }
                },
                parent: {
                  label: 'Parent Domain',
                  validation: { required: true }
                }
              }
            }
          }
        },
        tabs: {
          details: {
            title: 'Details',
            fields: [
              {
                name: { label: 'Name', isEditable: true }
              },
              {
                accounts: { label: 'Accounts' },
                instances: { label: 'Instances' },
                volumes: { label: 'Volumes' }
              }
            ],
            dataProvider: function(args) {
              args.response.success({
                data: {
                  name: 'Domain name',
                  accounts: 12,
                  volumes: 23
                }
              });
            }
          },
          adminAccounts: {
            title: 'Admin Accounts',
            multiple: true,
            fields: [
              {
                name: { label: 'Name' },
                vmtotal: { label: 'VMs' },
                iptotal: { label: 'IPs' },
                receivedbytes: { label: 'Bytes received' },
                sentbytes: { label: 'Bytes sent' },
                state: { label: 'State' }
              }
            ],
            dataProvider: function(args) {
              args.response.success({
                data: $.grep(testData.data.accounts, function(item, index) {
                  return item.domain === 'ROOT' && index <= 5;
                })
              });
            }
          },
          resourceLimits: {
            title: 'Resource Limits',
            fields: {
              vmlimit: { label: 'Instance Limit' },
              iplimit: { label: 'Public IP Limit' },
              volumelimit: { label: 'Volume Limit' },
              snapshotlimit: { label: 'Snapshot Limit' },
              templatelimit: { label: 'Template Limit' }
            },
            dataProvider: function(args) {
              args.response.success({
                data: testData.data.accounts[4]
              });
            }
          }
        }
      },
      labelField: 'name',
      dataProvider: function(args) {
        args.response.success({
          data: [
            { id: 'domainA', name: 'Domain A' },
            { id: 'domainB', name: 'Domain B' }
          ]
        });
      }
    }
  };
})(cloudStack);
