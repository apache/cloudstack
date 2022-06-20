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

from component.test_vpc_vm_life_cycle import Services


class TestVMLifeCycleSharedNwVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMLifeCycleSharedNwVPC, cls).getClsTestClient()
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

        cls.shared_nw_off = NetworkOffering.create(
                                        cls.api_client,
                                        cls.services["network_off_shared"],
                                        conservemode=False
                                        )
        # Enable Network offering
        cls.shared_nw_off.update(cls.api_client, state='Enabled')


        physical_network, shared_vlan = get_free_vlan(cls.api_client, cls.zone.id)
        if shared_vlan is None:
            assert False, "Failed to get free vlan id for shared network creation in the zone"

        #create network using the shared network offering created
        cls.services["network"]["acltype"] = "Domain"
        cls.services["network"]["physicalnetworkid"] = physical_network.id
        cls.services["network"]["vlan"] = shared_vlan

        # Start Ip and End Ip should be specified for shared network
        cls.services["network"]["startip"] = '10.1.2.20'
        cls.services["network"]["endip"] = '10.1.2.30'

        # Creating network using the network offering created
        cls.network_2 = Network.create(
                                cls.api_client,
                                cls.services["network"],
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                networkofferingid=cls.shared_nw_off.id,
                                zoneid=cls.zone.id,
                                gateway='10.1.2.1',
                                )

        cls.vm_1 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id),
                                              str(cls.network_2.id)]
                                  )

        cls.vm_2 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id),
                                              str(cls.network_2.id)]
                                  )


        cls.vm_3 = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering.id,
                                  networkids=[str(cls.network_1.id),
                                              str(cls.network_2.id)]
                                  )

        cls.public_ip_1 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )
        cls.lb_rule = LoadBalancerRule.create(
                                    cls.api_client,
                                    cls.services["lbrule"],
                                    ipaddressid=cls.public_ip_1.ipaddress.id,
                                    accountid=cls.account.name,
                                    networkid=cls.network_1.id,
                                    vpcid=cls.vpc.id,
                                    domainid=cls.account.domainid
                                )

        # Only the vms in the same network can be added to load balancing rule
        # hence we can't add vm_2 with vm_1
        cls.lb_rule.assign(cls.api_client, [cls.vm_1])

        cls.public_ip_2 = PublicIPAddress.create(
                                cls.api_client,
                                accountid=cls.account.name,
                                zoneid=cls.zone.id,
                                domainid=cls.account.domainid,
                                networkid=cls.network_1.id,
                                vpcid=cls.vpc.id
                                )

        cls.nat_rule = NATRule.create(
                                  cls.api_client,
                                  cls.vm_1,
                                  cls.services["natrule"],
                                  ipaddressid=cls.public_ip_2.ipaddress.id,
                                  openfirewall=False,
                                  networkid=cls.network_1.id,
                                  vpcid=cls.vpc.id
                                  )

        # Opening up the ports in VPC
        cls.nwacl_nat = NetworkACL.create(
                                         cls.api_client,
                                         networkid=cls.network_1.id,
                                         services=cls.services["natrule"],
                                         traffictype='Ingress'
                                    )

        cls.nwacl_lb = NetworkACL.create(
                                cls.api_client,
                                networkid=cls.network_1.id,
                                services=cls.services["lbrule"],
                                traffictype='Ingress'
                                )
        cls.services["icmp_rule"]["protocol"] = "all"
        cls.nwacl_internet_1 = NetworkACL.create(
                                        cls.api_client,
                                        networkid=cls.network_1.id,
                                        services=cls.services["icmp_rule"],
                                        traffictype='Egress'
                                        )
        cls._cleanup = [
                        cls.account,
                        cls.network_2,
                        cls.nw_off,
                        cls.shared_nw_off,
                        cls.vpc_off,
                        cls.service_offering,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.vpc_off.update(cls.api_client, state='Disabled')
            cls.shared_nw_off.update(cls.api_client, state='Disabled')
            cls.nw_off.update(cls.api_client, state='Disabled')
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def validate_vpc_offering(self, vpc_offering):
        """Validates the VPC offering"""

        self.debug("Check if the VPC offering is created successfully?")
        vpc_offs = VpcOffering.list(
                                    self.apiclient,
                                    id=vpc_offering.id
                                    )
        self.assertEqual(
                         isinstance(vpc_offs, list),
                         True,
                         "List VPC offerings should return a valid list"
                         )
        self.assertEqual(
                 vpc_offering.name,
                 vpc_offs[0].name,
                "Name of the VPC offering should match with listVPCOff data"
                )
        self.debug(
                "VPC offering is created successfully - %s" %
                                                        vpc_offering.name)
        return

    def validate_vpc_network(self, network, state=None):
        """Validates the VPC network"""

        self.debug("Check if the VPC network is created successfully?")
        vpc_networks = VPC.list(
                                    self.apiclient,
                                    id=network.id
                          )
        self.assertEqual(
                         isinstance(vpc_networks, list),
                         True,
                         "List VPC network should return a valid list"
                         )
        self.assertEqual(
                 network.name,
                 vpc_networks[0].name,
                "Name of the VPC network should match with listVPC data"
                )
        if state:
            self.assertEqual(
                 vpc_networks[0].state,
                 state,
                "VPC state should be '%s'" % state
                )
        self.debug("VPC network validated - %s" % network.name)
        return

    def validate_network_rules(self):
        """Validating if the network rules (PF/LB) works properly or not?"""

        try:
            self.debug("Checking if we can SSH into VM_1 through %s?" %
                    (self.public_ip_1.ipaddress.ipaddress))
            ssh_1 = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")

            self.debug("Verifying if we can ping to outside world from VM?")
            # Ping to outsite world
            res = ssh_1.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
            result = str(res)
            self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

            self.debug("We should be allowed to ping virtual gateway")
            self.debug("Finding the gateway corresponding to isolated network")
            gateways = [nic.gateway for nic in self.vm_1.nic if nic.networkid == self.network_1.id]

            gateway_list_validation_result = validateList(gateways)

            self.assertEqual(gateway_list_validation_result[0], PASS, "gateway list validation failed due to %s" %
                             gateway_list_validation_result[2])

            gateway = gateway_list_validation_result[1]

            self.debug("VM gateway: %s" % gateway)

            res = ssh_1.execute("ping -c 1 %s" % gateway)
            self.debug("ping -c 1 %s: %s" % (gateway, res))

            result = str(res)
            self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to VM gateway should be successful"
                         )
        except Exception as e:
            self.fail("Failed to SSH into VM - %s, %s" %
                                    (self.public_ip_1.ipaddress.ipaddress, e))
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_01_deploy_instance_in_network(self):
        """ Test deploy an instance in VPC networks
        """

        # Validate the following
        # 1. Successful deployment of the User VM.
        # 2. Ping any host in the public Internet successfully.
        # 3. Ping the gateways of the VPC's guest network and the
        #    Shared Guest Network successfully.

        self.debug("Check if deployed VMs are in running state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VMs should return a valid response"
                         )
        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_02_stop_instance_in_network(self):
        """ Test stop an instance in VPC networks
        """

        # Validate the following
        # 1. Stop the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Stopping one of the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_2.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_03_start_instance_in_network(self):
        """ Test start an instance in VPC networks
        """

        # Validate the following
        # 1. Start the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Starting one of the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Virtual machine should be in running state"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_04_reboot_instance_in_network(self):
        """ Test reboot an instance in VPC networks
        """

        # Validate the following
        # 1. Reboot the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Restarting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_1.reboot(self.apiclient)
            self.vm_2.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual instances, %s" % e)

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        for vm in vms:
            self.assertEqual(
                         vm.state,
                         "Running",
                         "Virtual machine should be in running state"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_05_destroy_instance_in_network(self):
        """ Test destroy an instance in VPC networks
        """

        # Validate the following
        # 1. Destroy one of the virtual machines.
        # 2. Rules should be still configured on virtual router.

        self.debug("Destroying one of the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_2.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        #Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         vms,
                         None,
                         "List virtual machines should not return anything"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_06_recover_instance_in_network(self):
        """ Test recover an instance in VPC networks
        """

        self.debug("Deploying vm")

        self.vm_2 = VirtualMachine.create(
                                  self.api_client,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id,
                                  networkids=[str(self.network_1.id),
                                              str(self.network_2.id)]
                                  )

        self.cleanup.append(self.vm_2)

        try:
            self.vm_2.delete(self.apiclient, expunge=False)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        try:
            self.vm_2.recover(self.apiclient)
        except Exception as e:
            self.fail("Failed to recover the virtual instances, %s" % e)

        self.debug("Check if the instance is in stopped state?")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Stopped",
                         "Virtual machine should be in stopped state"
                         )

        self.debug("Starting the instance: %s" % self.vm_2.name)
        try:
            self.vm_2.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the instances, %s" % e)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_2.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List virtual machines should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Virtual machine should be in running state"
                         )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_07_migrate_instance_in_network(self):
        """ Test migrate an instance in VPC networks
        """

        # Validate the following
        # 1. Migrate the virtual machines to other hosts
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        host = findSuitableHostForMigration(self.apiclient, self.vm_1.id)
        if host is None:
            self.skipTest(ERROR_NO_HOST_FOR_MIGRATION)

        self.debug("Migrating VM-ID: %s to Host: %s" % (
                                                        self.vm_1.id,
                                                        host.id
                                                        ))

        try:
            self.vm_1.migrate(self.apiclient, hostid=host.id)
        except Exception as e:
            self.fail("Failed to migrate instance, %s" % e)

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_08_user_data(self):
        """ Test user data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the user data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR

        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")
            ssh.execute("yum install wget -y")
        except Exception as e:
            self.fail("Failed to SSH into instance")

        self.debug("check the userdata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/user-data" % self.network_1.gateway,
               "cat user-data",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertEqual(
                            res.count(
                                self.services["virtual_machine"]["userdata"]),
                            1,
                            "Verify user data from router"
                        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_09_meta_data(self):
        """ Test meta data in virtual machines
        """

        # Validate the following
        # 1. Create a VPC with cidr - 10.1.1.1/16
        # 2. Add network1(10.1.1.1/24) and network2(10.1.2.1/24) to this VPC.
        # 3. Deploy a vm in network1 and a vm in network2 using userdata
        # Steps
        # 1.Query for the meta data for both the user vms from both networks
        #   User should be able to query the user data for the vms belonging to
        #   both the networks from the VR

        try:
            ssh = self.vm_1.get_ssh_client(
                                ipaddress=self.public_ip_1.ipaddress.ipaddress,
                                reconnect=True)
            self.debug("SSH into VM is successfully")
        except Exception as e:
            self.fail("Failed to SSH into instance")

        self.debug("check the metadata with that of present in router")
        try:
            cmds = [
               "wget http://%s/latest/vm-id" % self.network_1.gateway,
               "cat vm-id",
               ]
            for c in cmds:
                result = ssh.execute(c)
                self.debug("%s: %s" % (c, result))
        except Exception as e:
            self.fail("Failed to SSH in Virtual machine: %s" % e)

        res = str(result)
        self.assertNotEqual(
                         res,
                         None,
                         "Meta data should be returned from router"
                        )
        return

    @attr(tags=["advanced", "intervlan"], required_hardware="true")
    def test_10_expunge_instance_in_network(self):
        """ Test expunge an instance in VPC networks
        """

        # Validate the following
        # 1. Recover the virtual machines.
        # 2. Vm should be in stopped state. State both the instances
        # 3. Make sure that all the PF,LB and Static NAT rules on this VM
        #    works as expected.
        # 3. Make sure that we are able to access google.com from this user Vm

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug("Delete virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_3.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        self.debug(
            "Waiting for expunge interval to cleanup the network and VMs")

        wait_for_cleanup(
                         self.apiclient,
                         ["expunge.interval", "expunge.delay"]
                        )

        self.debug("Validating if network rules are coonfigured properly?")
        self.validate_network_rules()

        self.debug(
            "Deleting the rest of the virtual machines in account: %s" %
                                                    self.account.name)
        try:
            self.vm_1.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to destroy the virtual instances, %s" % e)

        self.debug(
            "Waiting for expunge interval to cleanup the network and VMs")

        wait_for_cleanup(
                         self.apiclient,
                         ["expunge.interval", "expunge.delay"]
                        )

        # Check if the network rules still exists after Vm expunged
        self.debug("Checking if NAT rules existed ")
        with self.assertRaises(Exception):
            NATRule.list(
                         self.apiclient,
                         id=self.nat_rule.id,
                         listall=True
                         )

            LoadBalancerRule.list(
                                  self.apiclient,
                                  id=self.lb_rule.id,
                                  listall=True
                                  )
        return