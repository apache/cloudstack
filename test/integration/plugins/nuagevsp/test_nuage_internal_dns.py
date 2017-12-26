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

""" Component tests for Internal DNS functionality with Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.cloudstackAPI import updateZone
from marvin.lib.base import Account, Network
# Import System Modules
from nose.plugins.attrib import attr
import time


class TestNuageInternalDns(nuageTestCase):
    DNS = "06"
    HOSTNAME = "0c"
    DOMAINNAME = "0f"

    @classmethod
    def setUpClass(cls):
        super(TestNuageInternalDns, cls).setUpClass()
        cls.dnsdata = cls.test_data["nuagevsp"]
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.account = Account.create(
            self.apiclient,
            self.test_data["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.test_data["virtual_machine"]["displayname"] = "vm1"
        self.test_data["virtual_machine"]["name"] = "vm1"

        self.cleanup = [self.account]
        return

    # Creates and verifies the firewall rule
    def create_and_verify_fw(self, vm, public_ip, network):
        self.debug("Create and verify firewall rule")
        self.create_StaticNatRule_For_VM(vm, public_ip, network)

        # VSD verification
        self.verify_vsd_floating_ip(network, vm, public_ip.ipaddress)

        fw_rule = self.create_FirewallRule(
            public_ip, self.test_data["ingress_rule"])
        self.verify_vsd_firewall_rule(fw_rule)

    def verify_vsd_dhcp_option(self, dhcp_type, value, subnet_or_vm_interface,
                               is_vm_interface=False):
        self.debug("Verifying the creation and value of DHCP option type - %s "
                   "in VSD" % dhcp_type)
        found_dhcp_type = False
        if is_vm_interface:
            dhcp_options = self.vsd.get_vm_interface_dhcpoptions(
                filter=self.get_externalID_filter(subnet_or_vm_interface.id))
        else:
            dhcp_options = self.vsd.get_subnet_dhcpoptions(
                filter=self.get_externalID_filter(subnet_or_vm_interface.id))
        for dhcp_option in dhcp_options:
            self.debug("dhcptype option is %s:" % dhcp_option.actual_type)
            self.debug("dhcptype expected value is option is %s:" % dhcp_type)
            if dhcp_option.type == dhcp_type:
                found_dhcp_type = True
                if isinstance(dhcp_option.actual_values, list):
                    self.debug("dhcptype actual value is %s:" %
                               dhcp_option.actual_values)
                    if value in dhcp_option.actual_values:
                        self.debug("Excepted DHCP option value found in VSD")
                    else:
                        self.fail("Excepted DHCP option value not found in "
                                  "VSD")
                else:
                    self.assertEqual(dhcp_options.actual_values, value,
                                     "Expected DHCP option value is not same "
                                     "in both CloudStack and VSD"
                                     )
        if not found_dhcp_type:
            self.fail("Expected DHCP option type and value not found in the "
                      "VSD")
        self.debug("Successfully verified the creation and value of DHCP "
                   "option type - %s in VSD" % dhcp_type)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_01_Isolated_Network_with_zone(self):
        """ Verify InternalDns on Isolated Network
        """

        # Validate the following
        # 1. Create an Isolated network - network1 (10.1.1.1/24) by using DNS
        #    network offering.
        # 2. Deploy vm1 in network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.

        # update Network Domain at zone level
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "isolated.com"
        self.apiclient.updateZone(cmd)
        self.debug("Creating and enabling Nuage Vsp Isolated Network "
                   "offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        network_1 = self.create_Network(network_offering)
        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_vm(vm_1)

        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "isolated.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_02_Isolated_Network(self):
        """ Verify InternalDns on Isolated Network with ping by hostname
        """

        # Validate the following
        # 1. Create an Isolated network - network1 (10.1.1.1/24) by using DNS
        #    network offering.
        # 2. Deploy vm1 in network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.
        # 5. Deploy VM2 in network1.
        # 6. Verify end to end by pinging with hostname

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "isolated.com"
        self.apiclient.updateZone(cmd)

        self.debug("Creating and enabling Nuage Vsp Isolated Network "
                   "offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        network_1 = self.create_Network(network_offering)
        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_vm(vm_1)

        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "isolated.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        self.test_data["virtual_machine"]["displayname"] = "vm2"
        self.test_data["virtual_machine"]["name"] = "vm2"
        vm_2 = self.create_VM(network_1)
        self.test_data["virtual_machine"]["displayname"] = "vm1"
        self.test_data["virtual_machine"]["name"] = "vm1"
        self.verify_vsd_vm(vm_2)
        for nic in vm_2.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm2", nic, True)

        public_ip_1 = self.acquire_PublicIPAddress(network_1)
        self.create_and_verify_fw(vm_1, public_ip_1, network_1)

        vm_public_ip = public_ip_1.ipaddress.ipaddress

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.isolated.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_03_Isolated_Network_restarts(self):
        """ Verify InternalDns on Isolated Network with restart networks and
        ping by hostname
        """

        # Validate the following
        # 1. Create an Isolated network - network1 (10.1.1.1/24) by using DNS
        #    network offering.
        # 2. Deploy vm1 in network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.
        # 5. Deploy VM2 in network1.
        # 6. Verify end to end by pinging with hostname while restarting
        #    network1 without and with cleanup.

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "isolated.com"
        self.apiclient.updateZone(cmd)

        self.debug("Creating and enabling Nuage Vsp Isolated Network "
                   "offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        network_1 = self.create_Network(network_offering)
        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_vm(vm_1)

        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "isolated.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        self.test_data["virtual_machine"]["displayname"] = "vm2"
        self.test_data["virtual_machine"]["name"] = "vm2"
        vm_2 = self.create_VM(network_1)
        self.test_data["virtual_machine"]["displayname"] = "vm1"
        self.test_data["virtual_machine"]["name"] = "vm1"
        self.verify_vsd_vm(vm_2)
        for nic in vm_2.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm2", nic, True)

        public_ip_1 = self.acquire_PublicIPAddress(network_1)
        self.create_and_verify_fw(vm_1, public_ip_1, network_1)

        vm_public_ip = public_ip_1.ipaddress.ipaddress

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        time.sleep(30)
        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.isolated.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

        # Restarting Isolated network (cleanup = false)
        self.debug("Restarting the created Isolated network without "
                   "cleanup...")
        Network.restart(network_1, self.api_client, cleanup=False)
        self.validate_Network(network_1, state="Implemented")
        vr = self.get_Router(network_1)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)
        self.verify_vsd_vm(vm_2)

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.isolated.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

        # Restarting Isolated network (cleanup = true)
        self.debug("Restarting the created Isolated network with cleanup...")
        Network.restart(network_1, self.api_client, cleanup=True)
        self.validate_Network(network_1, state="Implemented")
        vr = self.get_Router(network_1)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)
        self.verify_vsd_vm(vm_2)

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.isolated.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_04_Update_Network_with_Domain(self):
        """ Verify update NetworkDomain for InternalDns on Isolated Network
        """

        # Validate the following
        # 1. Create an Isolated network - network1 (10.1.1.1/24) by using DNS
        #    network offering.
        # 2. Deploy vm1 in network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.
        # 5. Update Network domain and verify it is properly updated

        # update Network Domain at zone level
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "isolated.com"
        self.apiclient.updateZone(cmd)

        self.debug("Creating and enabling Nuage Vsp Isolated Network "
                   "offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        network_1 = self.create_Network(network_offering)
        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_vm(vm_1)

        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "isolated.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        update_response = Network.update(
            network_1, self.apiclient, id=network_1.id,
            networkdomain="update.com", changecidr=False)
        completeoutput = str(update_response).strip('[]')
        self.debug("network update response is " + completeoutput)
        self.assertEqual("update.com", update_response.networkdomain,
                         "Network Domain is not updated as expected"
                         )
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "update.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "update.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_05_Update_Network_with_Domain(self):
        """ Verify update NetworkDomain for InternalDns on Isolated Network
        with ping VM
        """

        # Validate the following
        # 1. Create an Isolated network - network1 (10.1.1.1/24) by using DNS
        #    network offering.
        # 2. Deploy vm1 in network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.
        # 5. Update Network domain and verify it is properly updated

        # update Network Domain at zone level
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "isolated.com"
        self.apiclient.updateZone(cmd)

        self.debug("Creating and enabling Nuage Vsp Isolated Network "
                   "offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["isolated_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")

        network_1 = self.create_Network(network_offering)
        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1)
        self.verify_vsd_vm(vm_1)

        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(
            self.DOMAINNAME, "isolated.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "isolated.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        update_response = Network.update(
            network_1, self.apiclient, id=network_1.id,
            networkdomain="update.com", changecidr=False)
        completeoutput = str(update_response).strip('[]')
        self.debug("network update response is " + completeoutput)
        self.assertEqual("update.com", update_response.networkdomain,
                         "Network Domain is not updated as expected"
                         )
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "update.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "update.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        # stop and start VM to get new DHCP option
        try:
            vm_1.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop the virtual instances, %s" % e)

        self.test_data["virtual_machine"]["displayname"] = "vm2"
        self.test_data["virtual_machine"]["name"] = "vm2"
        vm_2 = self.create_VM(network_1)
        self.test_data["virtual_machine"]["displayname"] = "vm1"
        self.test_data["virtual_machine"]["name"] = "vm1"
        self.verify_vsd_vm(vm_2)
        for nic in vm_2.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(
                self.DOMAINNAME, "update.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm2", nic, True)

        try:
            vm_1.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start the virtual instances, %s" % e)

        public_ip_1 = self.acquire_PublicIPAddress(network_1)
        self.create_and_verify_fw(vm_1, public_ip_1, network_1)

        vm_public_ip = public_ip_1.ipaddress.ipaddress

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception: %s " % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.update.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="false")
    def test_06_VPC_Network_With_InternalDns(self):
        """ Verify InternalDns on VPC Network
        """

        # Validate the following
        # 1. Create a VPC and tier network by using DNS network offering.
        # 2. Deploy vm1 in tier network.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.apiclient.updateZone(cmd)
        vpc_off = self.create_VpcOffering(self.dnsdata["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")

        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16', cleanup=False)

        self.debug("Creating Nuage Vsp VPC Network offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["vpc_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")
        network_1 = self.create_Network(
            network_offering, gateway='10.1.1.1', vpc=vpc)

        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_vm(vm_1)

        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_07_VPC_Network_With_InternalDns(self):
        """ Verify InternalDns on VPC Network by ping with hostname
        """

        # Validate the following
        # 1. Create a VPC and Tier network by using DNS network offering.
        # 2. Deploy vm1 in Tier network network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.
        # 5. Deploy Vm2.
        # 6. Verify end to end by pinging with hostname

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.apiclient.updateZone(cmd)

        vpc_off = self.create_VpcOffering(self.dnsdata["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16', cleanup=False)

        self.debug("Creating Nuage Vsp VPC Network offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["vpc_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")
        network_1 = self.create_Network(
            network_offering, gateway='10.1.1.1', vpc=vpc)

        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_vm(vm_1)
        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        self.test_data["virtual_machine"]["displayname"] = "vm2"
        self.test_data["virtual_machine"]["name"] = "vm2"
        vm_2 = self.create_VM(network_1)
        self.test_data["virtual_machine"]["displayname"] = "vm1"
        self.test_data["virtual_machine"]["name"] = "vm1"
        self.verify_vsd_vm(vm_2)
        for nic in vm_2.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm2", nic, True)

        public_ip_1 = self.acquire_PublicIPAddress(network_1, vpc)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule "
                   "(SSH) accessible...")
        public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=network_1)

        # VSD verification
        self.verify_vsd_firewall_rule(public_ssh_rule)
        vm_public_ip = public_ip_1.ipaddress.ipaddress

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.vpc.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

    @attr(tags=["advanced", "nuagevsp"], required_hardware="true")
    def test_08_VPC_Network_Restarts_With_InternalDns(self):
        """ Verify InternalDns on VPC Network with restarts and ping by
        hostname
        """

        # Validate the following
        # 1. Create a VPC and Tier network by using DNS network offering.
        # 2. Deploy vm1 in Tier network network1.
        # 3. Verify dhcp option 06 and 0f for subnet
        # 4. Verify dhcp option 06,15 and 0f for vm Interface.
        # 5. Deploy Vm2.
        # 6. Verify end to end by pinging with hostname while restarting
        #    VPC and Tier without and with cleanup.

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.apiclient.updateZone(cmd)

        vpc_off = self.create_VpcOffering(self.dnsdata["vpc_offering"])
        self.validate_VpcOffering(vpc_off, state="Enabled")
        vpc = self.create_Vpc(vpc_off, cidr='10.1.0.0/16', cleanup=False)

        self.debug("Creating Nuage Vsp VPC Network offering...")
        network_offering = self.create_NetworkOffering(
            self.dnsdata["vpc_network_offering"])
        self.validate_NetworkOffering(network_offering, state="Enabled")
        network_1 = self.create_Network(
            network_offering, gateway='10.1.1.1', vpc=vpc)

        vm_1 = self.create_VM(network_1)

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_vm(vm_1)
        # Internal DNS check point on VSD
        self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", network_1)
        self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", network_1)
        for nic in vm_1.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm1", nic, True)

        self.test_data["virtual_machine"]["displayname"] = "vm2"
        self.test_data["virtual_machine"]["name"] = "vm2"
        vm_2 = self.create_VM(network_1)
        self.test_data["virtual_machine"]["displayname"] = "vm1"
        self.test_data["virtual_machine"]["name"] = "vm1"
        self.verify_vsd_vm(vm_2)
        for nic in vm_2.nic:
            self.verify_vsd_dhcp_option(self.DNS, "10.1.1.2", nic, True)
            self.verify_vsd_dhcp_option(self.DOMAINNAME, "vpc.com", nic, True)
            self.verify_vsd_dhcp_option(self.HOSTNAME, "vm2", nic, True)

        public_ip_1 = self.acquire_PublicIPAddress(network_1, vpc)
        self.create_StaticNatRule_For_VM(vm_1, public_ip_1, network_1)
        # Adding Network ACL rule in the Public tier
        self.debug("Adding Network ACL rule to make the created NAT rule "
                   "(SSH) accessible...")
        public_ssh_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], network=network_1)

        # VSD verification
        self.verify_vsd_firewall_rule(public_ssh_rule)
        vm_public_ip = public_ip_1.ipaddress.ipaddress

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.vpc.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

        # Restarting VPC network (cleanup = false)
        self.debug("Restarting the created VPC network without cleanup...")
        Network.restart(network_1, self.api_client, cleanup=False)
        self.validate_Network(network_1, state="Implemented")
        vr = self.get_Router(network_1)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)
        self.verify_vsd_vm(vm_2)

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.vpc.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

        # Restarting VPC network (cleanup = true)
        self.debug("Restarting the created VPC network with cleanup...")
        Network.restart(network_1, self.api_client, cleanup=True)
        self.validate_Network(network_1, state="Implemented")
        vr = self.get_Router(network_1)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)
        self.verify_vsd_vm(vm_2)

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.vpc.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

        # Restarting VPC (cleanup = false)
        self.debug("Restarting the VPC without cleanup...")
        self.restart_Vpc(vpc, cleanup=False)
        self.validate_Network(network_1, state="Implemented")
        vr = self.get_Router(network_1)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.vpc.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)

        # Restarting VPC (cleanup = true)
        self.debug("Restarting the VPC with cleanup...")
        self.restart_Vpc(vpc, cleanup=True)
        self.validate_Network(network_1, state="Implemented")
        vr = self.get_Router(network_1)
        self.check_Router_state(vr, state="Running")
        self.check_VM_state(vm_1, state="Running")
        self.check_VM_state(vm_2, state="Running")

        # VSD verification
        self.verify_vsd_network(self.domain.id, network_1, vpc)
        self.verify_vsd_router(vr)
        self.verify_vsd_vm(vm_1)

        try:
            vm_1.ssh_ip = vm_public_ip
            vm_1.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_1.username = self.test_data["virtual_machine"]["username"]
            vm_1.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_1.ssh_ip, vm_1.password))

            ssh = vm_1.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        cmd = 'ping -c 2 vm2'
        self.debug("ping vm2 by hostname with command: " + cmd)
        outputlist = ssh.execute(cmd)
        self.debug("command is executed properly " + cmd)
        completeoutput = str(outputlist).strip('[]')
        self.debug("complete output is " + completeoutput)
        expectedlist = ['2 received', 'vm2.vpc.com', vm_2.ipaddress]
        for item in expectedlist:
            if item in completeoutput:
                self.debug("excepted value found in vm: " + item)
            else:
                self.fail("excepted value not found in vm: " + item)
