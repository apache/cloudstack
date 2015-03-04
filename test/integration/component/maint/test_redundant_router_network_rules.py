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

from nose.plugins.attrib import attr
from marvin.lib.base import *
from marvin.lib.utils import *
from marvin.lib.common import *

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import *
import time

class Services:
    """Test Services for customer defects
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
                                    "memory": 128,
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "disksize": 1
                        },
                        "virtual_machine": {
                                    "displayname": "Test VM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "static_nat": {
                                    "startport": 22,
                                    "endport": 22,
                                    "protocol": "TCP"
                        },
                        "network_offering": {
                                    "name": 'Network offering-RVR services',
                                    "displaytext": 'Network off-RVR services',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Vpn": 'VirtualRouter',
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                                    "serviceCapabilityList": {
                                        "SourceNat": {
                                            "SupportedSourceNatTypes": "peraccount",
                                            "RedundantRouter": "true",
                                        },
                                        "lb": {
                                               "SupportedLbIsolation": "dedicated"
                                        },
                                    },
                        },
                        "host": {
                                 "username": "root",
                                 "password": "password",
                                 "publicport": 22,
                        },
                        "network": {
                                  "name": "Test Network",
                                  "displaytext": "Test Network",
                                },
                        "lbrule": {
                                    "name": "SSH",
                                    "alg": "roundrobin",
                                    # Algorithm used for load balancing
                                    "privateport": 22,
                                    "publicport": 22,
                                    "openfirewall": True,
                                },
                        "natrule": {
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": "TCP"
                                },
                        "natrule_221": {
                                    "privateport": 22,
                                    "publicport": 221,
                                    "protocol": "TCP"
                                },
                        "fw_rule": {
                                    "startport": 1,
                                    "endport": 6000,
                                    "cidr": '55.55.0.0/11',
                                    # Any network (For creating FW rule)
                                    "protocol": 'TCP',
                                },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        "sleep": 60,
            }

class TestRedundantRouterRulesLifeCycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestRedundantRouterRulesLifeCycle, cls).getClsTestClient()
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
                                     self.apiclient,
                                     self.services["account"],
                                     admin=True,
                                     domainid=self.domain.id
                                     )
        self.cleanup = []
        self.cleanup.insert(0, self.account)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            #raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_networkRules_afterRebootRouters(self):
        """Test network rules after master & backup routers rebooted
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listStaticNats for the network associated
        # 7. listFirewallRules should show allowed ports open
        # 8. ssh to succeed to the guestVM
        # 9. listPublicIpAddresses for networkid should show acquired IP addr
        # 10. listPortForwardRules to show open ports 221, 222
        # 11. ssh should succeed for both ports
        # 12. listPublicIpAddresses for networkid should show acquired IP addr
        # 13 and 14. listLoadBalancerRules should show associated VMs for
        #    public IP
        # 15. ssh should succeed to the user VMs
        # 16. listRouters should show one Router in MASTER state and Running
        # 17. ssh should work for PF, FW, and LB ips
        # 18. listRouters should show both routers MASTER and BACKUP in
        #    Running state
        # 19. listPortForwardingRules, listFirewallRules, listLoadBalancerRule
        #    should return empty response
        # 20. listPublicIpAddresses should show now more addresses

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
                                self.apiclient,
                                id=network.id,
                                listall=True
                                )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
             )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
                    nw_response.state,
                    "Allocated",
                    "The network should be in allocated state after creation"
                    )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
            )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List Vms should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Vm should be in running state after deployment"
                         )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          associatednetworkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Starting router ID: %s" % master_router.id)

        for router in routers:
            try:
                self.debug("Rebooting router ID: %s" % master_router.id)
                #Stop the router
                cmd = rebootRouter.rebootRouterCmd()
                cmd.id = router.id
                self.apiclient.rebootRouter(cmd)
            except Exception as e:
                self.fail("Failed to reboot router..")

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_applyRules_restartRvRNetwork(self):
        """Test apply rules after network restart
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listStaticNats for the network associated
        # 7. listFirewallRules should show allowed ports open
        # 8. ssh to succeed to the guestVM
        # 9. listPublicIpAddresses for networkid should show acquired IP addr
        # 10. listPortForwardRules to show open ports 221, 222
        # 11. ssh should succeed for both ports
        # 12. listPublicIpAddresses for networkid should show acquired IP addr
        # 13 and 14. listLoadBalancerRules should show associated VMs for
        #    public IP
        # 15. ssh should succeed to the user VMs
        # 16. listRouters should show one Router in MASTER state and Running &
        #    one in BACKUP and Running
        # 17. ssh should work for PF, FW, and LB ips
        # 18. listRouters should show one Router in MASTER state and Running &
        #    one in BACKUP and Running
        # 19. ssh should work for PF, FW, and LB ips
        # 20. listPortForwardingRules, listFirewallRules, listLoadBalancerRule
        #     should return empty response
        # 21. listPublicIpAddresses should show now more addresses

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
                                self.apiclient,
                                id=network.id,
                                listall=True
                                )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
             )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
                    nw_response.state,
                    "Allocated",
                    "The network should be in allocated state after creation"
                    )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
            )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List Vms should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Vm should be in running state after deployment"
                         )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          associatednetworkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Restarting network ID: %s with cleanup true" %
                                                                network.id)

        try:
            network.restart(self.apiclient, cleanup=True)
        except Exception as e:
                self.fail("Failed to cleanup network")

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Restarting network ID: %s with cleanup false" %
                                                                network.id)

        try:
            network.restart(self.apiclient, cleanup=False)
        except Exception as e:
                self.fail("Failed to cleanup network")

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh"], required_hardware="true")
    def test_apply_and__delete_NetworkRulesOnRvR(self):
        """Test apply and delete network rules on redundant router
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP
        # 6. listRemoteAccessVpns for the network associated should show the
        #    VPN created
        # 7. listRemoteAccessVpns for the network associated should return
        #    empty response

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
                                self.apiclient,
                                id=network.id,
                                listall=True
                                )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
             )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
                    nw_response.state,
                    "Allocated",
                    "The network should be in allocated state after creation"
                    )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
            )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )
        self.debug("Deployed VM in network: %s" % network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List Vms should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Vm should be in running state after deployment"
                         )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          associatednetworkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
                         public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the Ip addr"
                         )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                            ipaddress=public_ip_2.ipaddress.ipaddress,
                            reconnect=True,
                            port=self.services["natrule_221"]["publicport"]
                           )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "ssh", "needle"], required_hardware="true")
    def test_applyNetworkRules_MasterDown_deleteNetworkRules(self):
        """Test apply network rules when master down and delete network rules
        """

        # Steps to validate
        # 1. listNetworks should show the created network in allocated state
        # 2. listRouters returns no running routers
        # 3. VMs should be deployed and in Running state
        # 4. should list MASTER and BACKUP routers
        # 5. listPublicIpAddresses for networkid should show acquired IP addr
        # 6. listStaticNats for the network associated
        # 7. listFirewallRules should show allowed ports open
        # 8. ssh to succeed to the guestVM
        # 9. listPublicIpAddresses for networkid should show acquired IP addr
        # 10. listPortForwardRules to show open ports 221, 222
        # 11. ssh should succeed for both ports
        # 12. listPublicIpAddresses for networkid should show acquired IP addr
        # 13 and 14. listLoadBalancerRules should show associated VMs for
        #    public IP
        # 15. ssh should succeed to the user VMs
        # 16. listRouters should show one Router in MASTER state and Running
        # 17. ssh should work for PF, FW, and LB ips
        # 18. listRouters should show both routers MASTER and BACKUP in
        #    Running state
        # 19. listPortForwardingRules, listFirewallRules, listLoadBalancerRule
        #    should return empty response
        # 20. listPublicIpAddresses should show now more addresses

        # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        network = Network.create(
                                self.apiclient,
                                self.services["network"],
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                networkofferingid=self.network_offering.id,
                                zoneid=self.zone.id
                                )
        self.debug("Created network with ID: %s" % network.id)

        networks = Network.list(
                                self.apiclient,
                                id=network.id,
                                listall=True
                                )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return a valid response for created network"
             )
        nw_response = networks[0]

        self.debug("Network state: %s" % nw_response.state)
        self.assertEqual(
                    nw_response.state,
                    "Allocated",
                    "The network should be in allocated state after creation"
                    )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
            routers,
            None,
            "Routers should not be spawned when network is in allocated state"
            )

        self.debug("Deploying VM in account: %s" % self.account.name)

        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(network.id)]
                                  )

        #Waiting for VM to come up before shutdown the router
        time.sleep(120)

        self.debug("Deployed VM in network: %s" % network.id)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List Vms should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Vm should be in running state after deployment"
                         )

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Stopping router ID: %s" % master_router.id)

        try:
            Router.stop(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to stop master router becaues of %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip.ipaddress.ipaddress,
                                        network.id
                                        ))
        self.debug("Enabling static NAT for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        try:
            static_nat = StaticNATRule.create(
                                    self.apiclient,
                                    self.services["fw_rule"],
                                    ipaddressid=public_ip.ipaddress.id
                                  )
            static_nat.enable(
                              self.apiclient,
                              ipaddressid=public_ip.ipaddress.id,
                              virtualmachineid=virtual_machine.id
                              )
            self.debug("Static NAT enabled for IP: %s" %
                                            public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("Failed to enable static NAT on IP: %s - %s" % (
                                            public_ip.ipaddress.ipaddress, e))

        public_ips = PublicIPAddress.list(
                                          self.apiclient,
                                          associatednetworkid=network.id,
                                          listall=True,
                                          isstaticnat=True
                                          )
        self.assertEqual(
                         isinstance(public_ips, list),
                         True,
                         "List public Ip for network should list the Ip addr"
                         )
        self.assertEqual(
            public_ips[0].ipaddress,
            public_ip.ipaddress.ipaddress,
            "Public Ip Address in the network created (%s) and listed (%s) do not match" % (
            public_ips[0].ipaddress, public_ip.ipaddress.ipaddress)
        )

        self.debug("creating a FW rule on IP: %s" %
                                    public_ip.ipaddress.ipaddress)
        fw_rule = FireWallRule.create(
                            self.apiclient,
                            ipaddressid=public_ip.ipaddress.id,
                            protocol='TCP',
                            cidrlist=[self.services["fw_rule"]["cidr"]],
                            startport=self.services["fw_rule"]["startport"],
                            endport=self.services["fw_rule"]["endport"]
                            )
        self.debug("Created a firewall rule on 22 port of IP: %s" %
                                            public_ip.ipaddress.ipaddress)

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                    ipaddress=public_ip.ipaddress.ipaddress)
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_2 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_2.ipaddress.ipaddress,
                                        network.id
                                        ))

        nat_rule = NATRule.create(
                                  self.apiclient,
                                  virtual_machine,
                                  self.services["natrule_221"],
                                  ipaddressid=public_ip_2.ipaddress.id,
                                  openfirewall=True
                                  )

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_2.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["natrule_221"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.name)
        public_ip_3 = PublicIPAddress.create(
                                self.apiclient,
                                accountid=self.account.name,
                                zoneid=self.zone.id,
                                domainid=self.account.domainid,
                                networkid=network.id
                                )
        self.debug("Associated %s with network %s" % (
                                        public_ip_3.ipaddress.ipaddress,
                                        network.id
                                        ))

        self.debug("Creating LB rule for IP address: %s" %
                                        public_ip_3.ipaddress.ipaddress)

        lb_rule = LoadBalancerRule.create(
                                    self.apiclient,
                                    self.services["lbrule"],
                                    ipaddressid=public_ip_3.ipaddress.id,
                                    accountid=self.account.name,
                                    networkid=network.id
                                )

        self.debug("Adding %s to the LB rule %s" % (
                                                virtual_machine.name,
                                                lb_rule.name
                                                ))
        lb_rule.assign(self.apiclient, [virtual_machine])

        self.debug("Trying to SSH into the virtual machine")
        try:
            virtual_machine.get_ssh_client(
                                ipaddress=public_ip_3.ipaddress.ipaddress,
                                reconnect=True,
                                port=self.services["lbrule"]["publicport"]
                               )
            self.debug("SSH to guest VM succeeded")
        except Exception as e:
            self.fail("SSH to guest VM failed: %s" % e)

        self.debug("Starting router ID: %s" % master_router.id)

        try:
            Router.start(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to start master router..")

        self.debug("Listing routers for network: %s" % network.name)
        routers = Router.list(
                              self.apiclient,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(
                    isinstance(routers, list),
                    True,
                    "list router should return Master and backup routers"
                    )
        self.assertEqual(
                    len(routers),
                    2,
                    "Length of the list router should be 2 (Backup & master)"
                    )
        for router in routers:
            self.assertEqual(
                             router.state,
                             "Running",
                             "Router state should be running"
                             )
        return
