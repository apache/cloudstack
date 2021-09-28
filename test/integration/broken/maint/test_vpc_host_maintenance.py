# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

""" Component tests VM life cycle in VPC network functionality
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import migrateSystemVm
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Host,
                             VirtualMachine,
                             ServiceOffering,
                             VPC,
                             VpcOffering,
                             Router,
                             Network,
                             NetworkOffering,
                             Cluster,
                             Account)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               wait_for_cleanup)
import time


class Services:
    """Test VM life cycle in VPC network services
    """

    def __init__(self):
        self.services = {
                         "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                                    },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    "memory": 64,
                                    },
                        "service_offering_1": {
                                    "name": "Tiny Instance- tagged host 1",
                                    "displaytext": "Tiny off-tagged host2",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    "memory": 64,
                                    "hosttags": "hosttag1"
                                    },
                         "service_offering_2": {
                                    "name": "Tiny Instance- tagged host 2",
                                    "displaytext": "Tiny off-tagged host2",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    "memory": 64,
                                    "hosttags": "hosttag2"
                                    },
                         "network_offering": {
                                    "name": 'VPC Network offering',
                                    "displaytext": 'VPC Network off',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Dhcp": 'VpcVirtualRouter',
                                            "Dns": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "Lb": 'VpcVirtualRouter',
                                            "UserData": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "NetworkACL": 'VpcVirtualRouter'
                                        },
                                },
                         "network_offering_no_lb": {
                                    "name": 'VPC Network offering',
                                    "displaytext": 'VPC Network off',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "serviceProviderList": {
                                            "Dhcp": 'VpcVirtualRouter',
                                            "Dns": 'VpcVirtualRouter',
                                            "SourceNat": 'VpcVirtualRouter',
                                            "PortForwarding": 'VpcVirtualRouter',
                                            "UserData": 'VpcVirtualRouter',
                                            "StaticNat": 'VpcVirtualRouter',
                                            "NetworkACL": 'VpcVirtualRouter'
                                        },
                                },
                         "network_off_shared": {
                                    "name": 'Shared Network offering',
                                    "displaytext": 'Shared Network offering',
                                    "guestiptype": 'Shared',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "useVpc": 'on',
                                    "specifyIpRanges": True,
                                    "specifyVlan": True
                                    },
                         "vpc_offering": {
                                    "name": 'VPC off',
                                    "displaytext": 'VPC off',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat',
                                },
                         "vpc": {
                                 "name": "TestVPC",
                                 "displaytext": "TestVPC",
                                 "cidr": '10.0.0.1/24'
                                 },
                         "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                  "netmask": '255.255.255.0',
                                  "limit": 5,
                                  # Max networks allowed as per hypervisor
                                  # Xenserver -> 5, VMWare -> 9
                                },
                         "lbrule": {
                                    "name": "SSH",
                                    "alg": "leastconn",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 2222,
                                    "openfirewall": False,
                                    "startport": 2222,
                                    "endport": 2222,
                                    "protocol": "TCP",
                                    "cidrlist": '0.0.0.0/0',
                                },
                         "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "startport": 22,
                                    "endport": 22,
                                    "protocol": "TCP",
                                    "cidrlist": '0.0.0.0/0',
                                },
                         "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '0.0.0.0/0',
                                    # Any network (For creating FW rule)
                                    "protocol": "TCP"
                                },
                         "http_rule": {
                                    "startport": 80,
                                    "endport": 80,
                                    "cidrlist": '0.0.0.0/0',
                                    "protocol": "ICMP"
                                },
                         "virtual_machine": {
                                    "displayname": "Test VM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    # Hypervisor type should be same as
                                    # hypervisor type of cluster
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                    "userdata": 'This is sample data',
                                },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 60,
                         "timeout": 30,
                         "mode": 'advanced'
                    }


class TestVMLifeCycleHostmaintenance(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleHostmaintenance, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("Template creation from root volume is not supported in LXC")
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        clusterWithSufficientHosts = None
        clusters = Cluster.list(cls.api_client, zoneid=cls.zone.id)
        for cluster in clusters:
            cls.hosts = Host.list(cls.api_client, clusterid=cluster.id)
            if len(cls.hosts) >= 2:
                clusterWithSufficientHosts = cluster
                break

        if clusterWithSufficientHosts is None:
            raise unittest.SkipTest("No Cluster with 2 hosts found")

        Host.update(cls.api_client, id=cls.hosts[0].id, hosttags="hosttag1")
        Host.update(cls.api_client, id=cls.hosts[1].id, hosttags="hosttag2")

        cls.service_offering_1 = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering_1"]
                                            )
        cls.service_offering_2 = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering_2"]
                                            )
        cls.vpc_off = VpcOffering.create(
                                     cls.api_client,
                                     cls.services["vpc_offering"]
                                     )
        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cls.vpc_off = VpcOffering.create(
                                     cls.api_client,
                                     cls.services["vpc_offering"]
                                     )

        cls.vpc_off.update(cls.api_client, state='Enabled')

        cls.services["vpc"]["cidr"] = '10.1.1.1/16'
        cls.vpc = VPC.create(
                         cls.api_client,
                         cls.services["vpc"],
                         vpcofferingid=cls.vpc_off.id,
                         zoneid=cls.zone.id,
                         account=cls.account.name,
                         domainid=cls.account.domainid
                         )

        cls.nw_off = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=False
                                            )
        # Enable Network offering
        cls.nw_off.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_1 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.1.1',
                                vpcid=cls.vpc.id
                                )
        cls.nw_off_no_lb = NetworkOffering.create(
                                    cls.api_client,
                                    cls.services["network_offering_no_lb"],
                                    conservemode=False
                                    )
        # Enable Network offering
        cls.nw_off_no_lb.update(cls.api_client, state='Enabled')

        # Creating network using the network offering created
        cls.network_2 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.nw_off_no_lb.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.2.1',
                                vpcid=cls.vpc.id
                                )
        # Spawn an instance in that network
        cls.vm_1 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering_1.id,
                                  networkids=[str(cls.network_1.id)]
                                  )
        # Spawn an instance in that network
        cls.vm_2 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering_1.id,
                                  networkids=[str(cls.network_1.id)]
                                  )
        cls.vm_3 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering_2.id,
                                  networkids=[str(cls.network_2.id)]
                                  )
        routers = Router.list(
                              cls.api_client,
                              account=cls.account.name,
                              domainid=cls.account.domainid,
                              listall=True
                              )
        if isinstance(routers, list):
            cls.vpcvr = routers[0]

        cls._cleanup = [
                        cls.service_offering_1,
                        cls.service_offering_2,
                        cls.nw_off,
                        cls.nw_off_no_lb,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Delete the host tags
            Host.update(cls.api_client, id=cls.hosts[0].id, hosttags="")
            Host.update(cls.api_client, id=cls.hosts[1].id, hosttags="")
            cls.account.delete(cls.api_client)
            wait_for_cleanup(cls.api_client, ["account.cleanup.interval"])
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
            cls.vpc_off.delete(cls.api_client)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.debug("Check the status of VPC virtual router")
        routers = Router.list(
                              self.apiclient,
                              networkid=self.network_1.id,
                              listall=True
                              )
        if not isinstance(routers, list):
            raise Exception("No response from list routers API")

        self.router = routers[0]
        if self.router.state == "Running":
            self.debug("Verified that the Router is in Running State")

        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def validate_vm_deployment(self):
        """Validates VM deployment on different hosts"""

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=self.network_1.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs shall return a valid response"
                         )
        host_1 = vms[0].hostid
        self.debug("Host for network 1: %s" % vms[0].hostid)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  networkid=self.network_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs shall return a valid response"
                         )
        host_2 = vms[0].hostid
        self.debug("Host for network 2: %s" % vms[0].hostid)

        self.assertNotEqual(
                host_1,
                host_2,
                "Both the virtual machines should be deployed on diff hosts "
                )
        return

    @attr(tags=["advanced", "intervlan"])
    def test_01_enable_maintenance_with_vpc_nw(self):
        """ Test enable Maintenance Mode on Hosts which have VPC elements
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy vm1 and vm2 in network1 and vm3 and vm4 in network2. Make
        #    sure vm1 and vm3 are deployed on one host in the cluster while
        #    vm2 and vm4 are deployed on the other host in the cluster. This
        #    can be done using host's tags & service offerings with host tags
        # Steps:
        # 1.Enable Maintenance on one of host on which VPCVR is present
        # Validations:
        # 1. Successfully push the host into maintenance mode.
        # 2. VMs present on the above host should successfully migrate to the
        #    other host present in the cluster

        Host.update(self.apiclient, id=self.hosts[0].id, hosttags="hosttag1,hosttag2")
        Host.update(self.apiclient, id=self.hosts[1].id, hosttags="hosttag1,hosttag2")
        
        self.validate_vm_deployment()
        self.debug("Stop the host on which the VPC virtual router is running")
        try:
            Host.enableMaintenance(self.apiclient, id=self.vpcvr.hostid)
        except Exception as e:
            self.fail("Failed to enable maintenance mode on host: %s" % e)

        self.debug(
            "Check if all instances belonging to the account are up again?")
        routers = Router.list(
                              self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True
                              )
        self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "List routers shall return a valid VPCVR for account"
                         )
        
        router = routers[0]
                    
        try:
            
            timeout = self.services["timeout"]
            self.debug("Timeout Value %d : " % timeout)
            while True:
                list_router_response = Router.list(
                    self.apiclient,
                    id = router.id,
                    state = "Running"
                )
                
                if list_router_response is not None:
                    break
                elif timeout == 0:
                    raise Exception("Router state should be running after migration")

                time.sleep(self.services["sleep"])
                timeout = timeout - 1
                self.debug("Waiting for %d seconds - %d tries left" % (self.services["sleep"],timeout))
            
            self.debug("Verified that the Router is in Running State")
            
        except Exception as e:
            self.fail("Failed to find the Router in Running state %s " % e)
                
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                    isinstance(vms, list),
                    True,
                    "VM response should return instances running for account"
                    )
        for vm in vms:
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running after migration"
                             )
        return

    @attr(tags=["advanced", "intervlan"])
    def test_02_cancel_maintenance(self):
        """ Test cancel Maintenance Mode on the above Hosts + Migrate VMs Back
        """

        # Steps
        # 1. Cancel Maintenance Mode on the host.
        # 2. Migrate the VMs back onto the host on which Maintenance mode is
        #    cancelled.
        # Validate the following
        # 1. Successfully cancel the Maintenance mode on the host.
        # 2. Migrate the VMs back successfully onto the host.
        # 3. Check that the network connectivity exists with the migrated VMs.
        
        try:
            timeout = self.services["timeout"]
            while True:
                list_host_response = Host.list(
                    self.apiclient,
                    id=self.vpcvr.hostid,
                    resourcestate="Maintenance")
                
                if list_host_response is not None:
                    break
                elif timeout == 0:
                    raise Exception("Failed to list the Host in Maintenance State")

                time.sleep(self.services["sleep"])
                timeout = timeout - 1
            
            self.debug("Verified that the Host is in Maintenance State")
            
        except:
            self.fail("Failed to find the Host in maintenance state")

        self.debug("Cancel host maintenence on which the VPCVR is running")
        try:
            Host.cancelMaintenance(self.apiclient, id=self.vpcvr.hostid)

            timeout = self.services["timeout"]
            while True:
                list_host_response = Host.list(
                    self.apiclient,
                    id=self.vpcvr.hostid,
                    state="Up")
                
                if list_host_response is not None:
                    break
                elif timeout == 0:
                    raise Exception("Failed to list the Host in Up State after Canceling Maintenance Mode")

                time.sleep(self.services["sleep"])
                timeout = timeout - 1
            
            self.debug("Verified that the Host is in Up State after Canceling Maintenance Mode")
            
        except Exception as e:
            self.fail("Failed to cancel maintenance mode on host: %s" % e)

        self.debug(
            "Migrating the instances back to the host: %s" %
                                                        self.vpcvr.hostid)
        try:
            cmd = migrateSystemVm.migrateSystemVmCmd()
            cmd.hostid = self.vpcvr.hostid
            cmd.virtualmachineid = self.vpcvr.id
            self.apiclient.migrateSystemVm(cmd)
        except Exception as e:
            self.fail("Failed to migrate VPCVR back: %s" % e)

        self.debug("Check the status of router after migration")
        routers = Router.list(
                              self.apiclient,
                              id=self.vpcvr.id,
                              listall=True
                              )
        self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "List routers shall return the valid response"
                         )
        self.assertEqual(
                         routers[0].state,
                         "Running",
                         "Router state should be running"
                         )
        #  TODO: Check for the network connectivity
        return

    @attr(tags=["advanced", "intervlan"])
    def test_03_reconnect_host(self):
        """ Test reconnect Host which has VPC elements
        """

        # Steps:
        # 1.Reconnect one of the host on which VPC Virtual Router is present.
        # Validate the following
        # 1. Host should successfully reconnect.
        # 2. Network connectivity to all the VMs on the host should not be
        #    effected due to reconnection.

        try:
            timeout = self.services["timeout"]        
            while True:
                list_host_response = Host.list(
                    self.apiclient,
                    id=self.vpcvr.hostid,
                    resourcestate="Enabled")
                
                if list_host_response is not None:
                    break
                elif timeout == 0:
                    raise Exception("Failed to list the Host in Up State")

                time.sleep(self.services["sleep"])
                timeout = timeout - 1
            
            self.debug("Verified that the Host is in Up State")
            
        except:
            self.fail("Failed to find the Host in Up State")

        self.debug("Reconnecting the host where VPC VR is running")
        try:
            Host.reconnect(self.apiclient, id=self.vpcvr.hostid)
        except Exception as e:
            self.fail("Failed to reconnect to host: %s" % e)

        self.debug("Check the status of router after migration")
        routers = Router.list(
                              self.apiclient,
                              id=self.vpcvr.id,
                              listall=True
                              )
        self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "List routers shall return the valid response"
                         )
        self.assertEqual(
                         routers[0].state,
                         "Running",
                         "Router state should be running"
                         )
        #  TODO: Check for the network connectivity
        return
