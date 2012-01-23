(function(cloudStack, $) {
  cloudStack.zoneWizard = {
    customUI: {
      publicTrafficIPRange: function(args) {
        var multiEditData = [];
        var totalIndex = 0;

        return $('<div>').multiEdit({
          context: args.context,
          noSelect: true,
          fields: {
            'gateway': { edit: true, label: 'Gateway' },
            'netmask': { edit: true, label: 'Netmask' },
            'vlanid': { edit: true, label: 'VLAN', isOptional: true },
            'startip': { edit: true, label: 'Start IP' },
            'endip': { edit: true, label: 'End IP' },
            'add-rule': { label: 'Add', addButton: true }
          },
          add: {
            label: 'Add',
            action: function(args) {
              multiEditData.push($.extend(args.data, {
                index: totalIndex
              }));

              totalIndex++;
              args.response.success();
            }
          },
          actions: {
            destroy: {
              label: 'Remove Rule',
              action: function(args) {
                multiEditData = $.grep(multiEditData, function(item) {
                  return item.index != args.context.multiRule[0].index
                });
                args.response.success();
              }
            }
          },
          dataProvider: function(args) {
            args.response.success({
              data: multiEditData
            });
          }
        });
      }
    },

    preFilters: {
      addPublicNetwork: function(args) {
        return args.data['network-model'] == 'Advanced';
      },

      addBasicPhysicalNetwork: function(args) {
        return args.data['network-model'] == 'Basic';
      },

      setupPhysicalNetwork: function(args) {
        return args.data['network-model'] == 'Advanced';
      }
    },

    forms: {
      zone: {
        preFilter: function(args) {
          var $form = args.$form;

          $form.find('input[name=security-groups-enabled]').change(function() {
            if ($(this).is(':checked')) {
              $form.find('[rel=networkOfferingIdWithoutSG]').hide();
              $form.find('[rel=networkOfferingIdWithSG]').show();
            } else {
              $form.find('[rel=networkOfferingIdWithoutSG]').show();
              $form.find('[rel=networkOfferingIdWithSG]').hide();
            }
          });

          if (args.data['network-model'] == 'Advanced') {
            args.$form.find('[rel=security-groups-enabled]').hide();
            args.$form.find('[rel=networkOfferingIdWithoutSG]').hide();
            args.$form.find('[rel=networkOfferingIdWithSG]').hide();
          } else {
            args.$form.find('[rel=security-groups-enabled]').show();
            args.$form.find('[rel=networkOfferingIdWithoutSG]').show();
            args.$form.find('[rel=networkOfferingIdWithSG]').show();

            $form.find('input[name=security-groups-enabled]:visible').trigger('change');
          }
        },
        fields: {
          name: { label: 'Name', validation: { required: true } },
          dns1: { label: 'DNS 1', validation: { required: true } },
          dns2: { label: 'DNS 2' },
          internaldns1: { label: 'Internal DNS 1', validation: { required: true } },
          internaldns2: { label: 'Internal DNS 2' },
          networkdomain: { label: 'Network Domain' },
          ispublic: { isBoolean: true, label: 'Public' },
          domain: {
            label: 'Domain',
            dependsOn: 'ispublic',
            isHidden: true,
            select: function(args) {
              args.response.success({
                data: $.map(testData.data.domains, function(domain) {
                  return {
                    id: domain.id,
                    description: domain.name
                  };
                })
              })
            }
          },
          'security-groups-enabled': {
            label: 'Security Groups Enabled',
            isBoolean: true,
            isReverse: true,
          },

          networkOfferingIdWithoutSG: {
            label: 'Network Offering',
            dependsOn: 'security-groups-enabled',
            select: function(args) {
              args.response.success({
                data: $.map(testData.data.networkOfferings, function(offering) {
                  return {
                    id: offering.id,
                    description: offering.name
                  };
                })
              });
            }
          },

          networkOfferingIdWithSG: {
            label: 'Network Offering',
            isHidden: true,
            select: function(args) {
              args.response.success({
                data: $.map(testData.data.networkOfferings, function(offering) {
                  return {
                    id: offering.id,
                    description: offering.name
                  };
                })
              })
            }
          }
        }
      },

      pod: {
        fields: {
          name: {
            label: 'Pod name',
            validation: { required: true }
          },
          reservedSystemGateway: {
            label: 'Reserved system gateway',
            validation: { required: true }
          },
          reservedSystemNetmask: {
            label: 'Reserved system netmask',
            validation: { required: true }
          },
          reservedSystemStartIp: {
            label: 'Start Reserved system IP',
            validation: { required: true }
          },
          reservedSystemEndIp: {
            label: 'End Reserved system IP',
            validation: { required: false }
          }
        }
      },

      basicPhysicalNetwork: {
        fields: {
          name: { label: 'Network name', validation: { required: true } },
          isStorageEnabled: { isBoolean: true, label: 'Storage traffic enabled' }
        }
      },

      guestTraffic: {
        preFilter: function(args) {
          var selectedZoneObj = {
            networktype: args.data['network-model']
          };

          if (selectedZoneObj.networktype == "Basic") {
            args.$form.find('[rel=vlanRange]').hide();
            args.$form.find('[rel=vlanId]').hide();
            args.$form.find('[rel=scope]').hide();
            args.$form.find('[rel=domainId]').hide();
            args.$form.find('[rel=account]').hide();
            args.$form.find('[rel=networkdomain]').hide();

            args.$form.find('[rel=podId]').show();
          } else {  // Advanced
            args.$form.find('[rel=vlanRange]').show();
            args.$form.find('[rel=vlanId]').show();
            args.$form.find('[rel=scope]').show();
            args.$form.find('[rel=domainId]').show();
            args.$form.find('[rel=account]').show();
            args.$form.find('[rel=networkdomain]').show();
          }
        },

        fields: {
          vlanRange: {
            label: 'VLAN Range',
            range: ['vlanRangeStart', 'vlanRangeEnd'],
            validation: { required: true }
          },
          name: {
            label: 'Name',
            validation: { required: true }
          },
          description: {
            label: 'Description',
            validation: { required: true }
          },
          vlanId: {
            label: "VLAN ID"
          },
          scope: {
            label: 'Scope',
            select: function(args) {
              var array1 = [];
              var selectedZoneObj = {
                securitygroupsenabled: args.context.zones[0]['security-groups-enabled']
              }
              if(selectedZoneObj.securitygroupsenabled) {
                array1.push({id: 'account-specific', description: 'Account'});
              }
              else {
                array1.push({id: 'zone-wide', description: 'All'});
                array1.push({id: 'domain-specific', description: 'Domain'});
                array1.push({id: 'account-specific', description: 'Account'});
              }
              args.response.success({ data: array1 });

              args.$select.change(function() {
                var $form = args.$select.closest('form');

                if($(this).val() == "zone-wide") {
                  $form.find('[rel=domainId]').hide();
                  $form.find('[rel=account]').hide();
                }
                else if ($(this).val() == "domain-specific") {
                  $form.find('[rel=domainId]').show();
                  $form.find('[rel=account]').hide();
                }
                else if($(this).val() == "account-specific") {
                  $form.find('[rel=domainId]').show();
                  $form.find('[rel=account]').show();
                }
              });

              setTimeout(function() {
                args.$select.trigger('change');
              });
            }
          },
          domainId: {
            label: 'Domain',
            select: function(args) {
              args.response.success({
                data: $.map(testData.data.domains, function(offering) {
                  return {
                    id: offering.id,
                    description: offering.name
                  };
                })
              });
            }
          },
          account: { label: 'Account' },
          guestGateway: { label: 'Guest gateway' },
          guestNetmask: { label: 'Guest netmask' },
          guestStartIp: { label: 'Guest start IP' },
          guestEndIp: { label: 'Guest end IP' },
          networkdomain: { label: 'Network domain' }
        }
      },
      cluster: {
        fields: {
          hypervisor: {
            label: 'Hypervisor',
            select: function(args) {
              args.response.success({ data:testData.data.hypervisors });
            }
          },
          name: {
            label: 'Cluster Name',
            validation: { required: true }
          },

          //hypervisor==VMWare begins here
          vCenterHost: {
            label: 'vCenter Host',
            validation: { required: true }
          },
          vCenterUsername: {
            label: 'vCenter Username',
            validation: { required: true }
          },
          vCenterPassword: {
            label: 'vCenter Password',
            validation: { required: true },
            isPassword: true
          },
          vCenterDatacenter: {
            label: 'vCenter Datacenter',
            validation: { required: true }
          }
          //hypervisor==VMWare ends here
        }
      },
      host: {
        preFilter: function(args) {
          var selectedClusterObj = {
            hypervisortype: args.data.hypervisor
          };

          var $form = args.$form;

          if(selectedClusterObj.hypervisortype == "VMware") {
            //$('li[input_group="general"]', $dialogAddHost).hide();
            $form.find('[rel=hostname]').hide();
            $form.find('[rel=username]').hide();
            $form.find('[rel=password]').hide();

            //$('li[input_group="vmware"]', $dialogAddHost).show();
            $form.find('[rel=vcenterHost]').css('display', 'block');

            //$('li[input_group="baremetal"]', $dialogAddHost).hide();
            $form.find('[rel=baremetalCpuCores]').hide();
            $form.find('[rel=baremetalCpu]').hide();
            $form.find('[rel=baremetalMemory]').hide();
            $form.find('[rel=baremetalMAC]').hide();

            //$('li[input_group="Ovm"]', $dialogAddHost).hide();
            $form.find('[rel=agentUsername]').hide();
            $form.find('[rel=agentPassword]').hide();
          }
          else if (selectedClusterObj.hypervisortype == "BareMetal") {
            //$('li[input_group="general"]', $dialogAddHost).show();
            $form.find('[rel=hostname]').css('display', 'block');
            $form.find('[rel=username]').css('display', 'block');
            $form.find('[rel=password]').css('display', 'block');

            //$('li[input_group="baremetal"]', $dialogAddHost).show();
            $form.find('[rel=baremetalCpuCores]').css('display', 'block');
            $form.find('[rel=baremetalCpu]').css('display', 'block');
            $form.find('[rel=baremetalMemory]').css('display', 'block');
            $form.find('[rel=baremetalMAC]').css('display', 'block');

            //$('li[input_group="vmware"]', $dialogAddHost).hide();
            $form.find('[rel=vcenterHost]').hide();

            //$('li[input_group="Ovm"]', $dialogAddHost).hide();
            $form.find('[rel=agentUsername]').hide();
            $form.find('[rel=agentPassword]').hide();
          }
          else if (selectedClusterObj.hypervisortype == "Ovm") {
            //$('li[input_group="general"]', $dialogAddHost).show();
            $form.find('[rel=hostname]').css('display', 'block');
            $form.find('[rel=username]').css('display', 'block');
            $form.find('[rel=password]').css('display', 'block');

            //$('li[input_group="vmware"]', $dialogAddHost).hide();
            $form.find('[rel=vcenterHost]').hide();

            //$('li[input_group="baremetal"]', $dialogAddHost).hide();
            $form.find('[rel=baremetalCpuCores]').hide();
            $form.find('[rel=baremetalCpu]').hide();
            $form.find('[rel=baremetalMemory]').hide();
            $form.find('[rel=baremetalMAC]').hide();

            //$('li[input_group="Ovm"]', $dialogAddHost).show();
            $form.find('[rel=agentUsername]').css('display', 'block');
            $form.find('[rel=agentUsername]').find('input').val("oracle");
            $form.find('[rel=agentPassword]').css('display', 'block');
          }
          else {
            //$('li[input_group="general"]', $dialogAddHost).show();
            $form.find('[rel=hostname]').css('display', 'block');
            $form.find('[rel=username]').css('display', 'block');
            $form.find('[rel=password]').css('display', 'block');

            //$('li[input_group="vmware"]', $dialogAddHost).hide();
            $form.find('[rel=vcenterHost]').hide();

            //$('li[input_group="baremetal"]', $dialogAddHost).hide();
            $form.find('[rel=baremetalCpuCores]').hide();
            $form.find('[rel=baremetalCpu]').hide();
            $form.find('[rel=baremetalMemory]').hide();
            $form.find('[rel=baremetalMAC]').hide();

            //$('li[input_group="Ovm"]', $dialogAddHost).hide();
            $form.find('[rel=agentUsername]').hide();
            $form.find('[rel=agentPassword]').hide();
          }
        },
        fields: {
          hostname: {
            label: 'Host name',
            validation: { required: true },
            isHidden: true
          },

          username: {
            label: 'User name',
            validation: { required: true },
            isHidden: true
          },

          password: {
            label: 'Password',
            validation: { required: true },
            isHidden: true,
            isPassword: true
          },
          //input_group="general" ends here

          //input_group="VMWare" starts here
          vcenterHost: {
            label: 'ESX/ESXi Host',
            validation: { required: true },
            isHidden: true
          },
          //input_group="VMWare" ends here

          //input_group="BareMetal" starts here
          baremetalCpuCores: {
            label: '# of CPU Cores',
            validation: { required: true },
            isHidden: true
          },
          baremetalCpu: {
            label: 'CPU (in MHz)',
            validation: { required: true },
            isHidden: true
          },
          baremetalMemory: {
            label: 'Memory (in MB)',
            validation: { required: true },
            isHidden: true
          },
          baremetalMAC: {
            label: 'Host MAC',
            validation: { required: true },
            isHidden: true
          },
          //input_group="BareMetal" ends here

          //input_group="OVM" starts here
          agentUsername: {
            label: 'Agent Username',
            validation: { required: false },
            isHidden: true
          },
          agentPassword: {
            label: 'Agent Password',
            validation: { required: true },
            isHidden: true,
            isPassword: true
          },
          //input_group="OVM" ends here

          //always appear (begin)
          hosttags: {
            label: 'Host tags',
            validation: { required: false }
          }
          //always appear (end)
        }
      },
      primaryStorage: {
        preFilter: function(args) {},

        fields: {
          name: {
            label: 'Name',
            validation: { required: true }
          },

          protocol: {
            label: 'Protocol',
            validation: { required: true },
            select: function(args) {
              var selectedClusterObj = {
                hypervisortype: args.context.zones[0].hypervisor
              };

              if(selectedClusterObj == null)
                return;

              if(selectedClusterObj.hypervisortype == "KVM") {
                var items = [];
                items.push({id: "nfs", description: "nfs"});
                items.push({id: "SharedMountPoint", description: "SharedMountPoint"});
                args.response.success({data: items});
              }
              else if(selectedClusterObj.hypervisortype == "XenServer") {
                var items = [];
                items.push({id: "nfs", description: "nfs"});
                items.push({id: "PreSetup", description: "PreSetup"});
                items.push({id: "iscsi", description: "iscsi"});
                args.response.success({data: items});
              }
              else if(selectedClusterObj.hypervisortype == "VMware") {
                var items = [];
                items.push({id: "nfs", description: "nfs"});
                items.push({id: "vmfs", description: "vmfs"});
                args.response.success({data: items});
              }
              else if(selectedClusterObj.hypervisortype == "Ovm") {
                var items = [];
                items.push({id: "nfs", description: "nfs"});
                items.push({id: "ocfs2", description: "ocfs2"});
                args.response.success({data: items});
              }
              else {
                args.response.success({data:[]});
              }

              args.$select.change(function() {
                var $form = $(this).closest('form');

                var protocol = $(this).val();
                if(protocol == null)
                  return;

                if(protocol == "nfs") {
                  //$("#add_pool_server_container", $dialogAddPool).show();
                  $form.find('[rel=server]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_nfs_server").val("");
                  $form.find('[rel=server]').find(".value").find("input").val("");

                  //$('li[input_group="nfs"]', $dialogAddPool).show();
                  $form.find('[rel=path]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                  $form.find('[rel=path]').find(".name").find("label").text("Path:");

                  //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                  $form.find('[rel=iqn]').hide();
                  $form.find('[rel=lun]').hide();

                  //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                  $form.find('[rel=vCenterDataCenter]').hide();
                  $form.find('[rel=vCenterDataStore]').hide();
                }
                else if(protocol == "ocfs2") {//ocfs2 is the same as nfs, except no server field.
                  //$dialogAddPool.find("#add_pool_server_container").hide();
                  $form.find('[rel=server]').hide();
                  //$dialogAddPool.find("#add_pool_nfs_server").val("");
                  $form.find('[rel=server]').find(".value").find("input").val("");

                  //$('li[input_group="nfs"]', $dialogAddPool).show();
                  $form.find('[rel=path]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.path"]+":");
                  $form.find('[rel=path]').find(".name").find("label").text("Path:");

                  //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                  $form.find('[rel=iqn]').hide();
                  $form.find('[rel=lun]').hide();

                  //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                  $form.find('[rel=vCenterDataCenter]').hide();
                  $form.find('[rel=vCenterDataStore]').hide();
                }
                else if(protocol == "PreSetup") {
                  //$dialogAddPool.find("#add_pool_server_container").hide();
                  $form.find('[rel=server]').hide();
                  //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                  $form.find('[rel=server]').find(".value").find("input").val("localhost");

                  //$('li[input_group="nfs"]', $dialogAddPool).show();
                  $form.find('[rel=path]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_path_container").find("label").text(g_dictionary["label.SR.name"]+":");
                  $form.find('[rel=path]').find(".name").find("label").text("SR Name-Label:");

                  //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                  $form.find('[rel=iqn]').hide();
                  $form.find('[rel=lun]').hide();

                  //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                  $form.find('[rel=vCenterDataCenter]').hide();
                  $form.find('[rel=vCenterDataStore]').hide();
                }
                else if(protocol == "iscsi") {
                  //$dialogAddPool.find("#add_pool_server_container").show();
                  $form.find('[rel=server]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_nfs_server").val("");
                  $form.find('[rel=server]').find(".value").find("input").val("");

                  //$('li[input_group="nfs"]', $dialogAddPool).hide();
                  $form.find('[rel=path]').hide();

                  //$('li[input_group="iscsi"]', $dialogAddPool).show();
                  $form.find('[rel=iqn]').css('display', 'block');
                  $form.find('[rel=lun]').css('display', 'block');

                  //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                  $form.find('[rel=vCenterDataCenter]').hide();
                  $form.find('[rel=vCenterDataStore]').hide();
                }
                else if(protocol == "vmfs") {
                  //$dialogAddPool.find("#add_pool_server_container").show();
                  $form.find('[rel=server]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_nfs_server").val("");
                  $form.find('[rel=server]').find(".value").find("input").val("");

                  //$('li[input_group="nfs"]', $dialogAddPool).hide();
                  $form.find('[rel=path]').hide();

                  //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                  $form.find('[rel=iqn]').hide();
                  $form.find('[rel=lun]').hide();

                  //$('li[input_group="vmfs"]', $dialogAddPool).show();
                  $form.find('[rel=vCenterDataCenter]').css('display', 'block');
                  $form.find('[rel=vCenterDataStore]').css('display', 'block');
                }
                else if(protocol == "SharedMountPoint") {  //"SharedMountPoint" show the same fields as "nfs" does.
                  //$dialogAddPool.find("#add_pool_server_container").hide();
                  $form.find('[rel=server]').hide();
                  //$dialogAddPool.find("#add_pool_nfs_server").val("localhost");
                  $form.find('[rel=server]').find(".value").find("input").val("localhost");

                  //$('li[input_group="nfs"]', $dialogAddPool).show();
                  $form.find('[rel=path]').css('display', 'block');
                  $form.find('[rel=path]').find(".name").find("label").text("Path:");

                  //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                  $form.find('[rel=iqn]').hide();
                  $form.find('[rel=lun]').hide();

                  //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                  $form.find('[rel=vCenterDataCenter]').hide();
                  $form.find('[rel=vCenterDataStore]').hide();
                }
                else {
                  //$dialogAddPool.find("#add_pool_server_container").show();
                  $form.find('[rel=server]').css('display', 'block');
                  //$dialogAddPool.find("#add_pool_nfs_server").val("");
                  $form.find('[rel=server]').find(".value").find("input").val("");

                  //$('li[input_group="iscsi"]', $dialogAddPool).hide();
                  $form.find('[rel=iqn]').hide();
                  $form.find('[rel=lun]').hide();

                  //$('li[input_group="vmfs"]', $dialogAddPool).hide();
                  $form.find('[rel=vCenterDataCenter]').hide();
                  $form.find('[rel=vCenterDataStore]').hide();
                }
              });

              args.$select.trigger("change");
            }
          },
          server: {
            label: 'Server',
            validation: { required: true },
            isHidden: true
          },

          //nfs
          path: {
            label: 'Path',
            validation: { required: true },
            isHidden: true
          },

          //iscsi
          iqn: {
            label: 'Target IQN',
            validation: { required: true },
            isHidden: true
          },
          lun: {
            label: 'LUN #',
            validation: { required: true },
            isHidden: true
          },

          //vmfs
          vCenterDataCenter: {
            label: 'vCenter Datacenter',
            validation: { required: true },
            isHidden: true
          },
          vCenterDataStore: {
            label: 'vCenter Datastore',
            validation: { required: true },
            isHidden: true
          },

          //always appear (begin)
          storageTags: {
            label: 'Storage Tags',
            validation: { required: false }
          }
          //always appear (end)
        }
      },
      secondaryStorage: {
        fields: {
          nfsServer: {
            label: 'NFS Server',
            validation: { required: true }
          },
          path: {
            label: 'Path',
            validation: { required: true }
          }
        }
      }
    },

    action: function(args) {
      var success = args.response.success;
      var error = args.response.error;
      var message = args.response.message;
      var data = args.data;
      var startFn = args.startFn;

      var stepFns = {
        addZone: function(args) {
          message('Creating zone');

          var zone = {};

          setTimeout(function() {
            stepFns.addPhysicalNetworks({
              data: $.extend(args.data, {
                zone: zone
              })
            });
          }, 300);
        },

        addPhysicalNetworks: function(args) {
          message('Creating physical network(s)');

          var physicalNetworks = [];

          setTimeout(function() {
            stepFns.addPod({
              data: $.extend(args.data, {
                physicalNetworks: physicalNetworks
              })
            });
          }, 400);
        },

        addPod: function(args) {
          message('Creating pod');

          var pod = {};

          setTimeout(function() {
            return error('addPod', 'System error 500', { fn: 'configurePublicTraffic', args: args });
            stepFns.configurePublicTraffic({
              data: $.extend(args.data, {
                pod: pod
              })
            });
          }, 300);
        },

        configurePublicTraffic: function(args) {
          message('Configuring public traffic');

          setTimeout(function() {
            stepFns.configureGuestTraffic({
              data: args.data
            });
          }, 200);
        },

        configureGuestTraffic: function(args) {
          message('Configuring guest traffic');

          setTimeout(function() {
            stepFns.addCluster({
              data: args.data
            });
          }, 200);
        },

        addCluster: function(args) {
          message('Creating cluster');

          var cluster = {};

          setTimeout(function() {
            return error('addCluster', 'System error 500', { fn: 'addHost', args: args });
            stepFns.addHost({
              data: $.extend(args.data, {
                cluster: cluster
              })
            });
          }, 200);
        },

        addHost: function(args) {
          message('Adding host');

          var host = {};

          setTimeout(function() {
            stepFns.addPrimaryStorage({
              data: $.extend(args.data, {
                host: host
              })
            });
          }, 400);
        },

        addPrimaryStorage: function(args) {
          message('Creating primary storage');

          setTimeout(function() {
            stepFns.addSecondaryStorage({
              data: args.data
            });
          }, 300);
        },

        addSecondaryStorage: function(args) {
          message('Creating secondary storage');

          setTimeout(function() {
            complete({
              data: data
            });
          }, 300);
        }
      };

      var complete = function(args) {
        message('Zone creation complete!');
        success({ data: { zone: { id: '123-4223' }}});
      };

      if (startFn) {
        stepFns[startFn.fn](startFn.args);
      } else {
        stepFns.addZone({});
      }
    },

    enableZoneAction: function(args) {
      setTimeout(function() {
        args.response.success();
      }, 2000);
    }
  };
}(cloudStack, jQuery));
