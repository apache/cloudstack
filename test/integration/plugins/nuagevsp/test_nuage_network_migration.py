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

""" Network migration test with Nuage VSP SDN plugin
"""
# Import Local Modules
from nuageTestCase import nuageTestCase
from marvin.lib.base import (Account, Host)
from marvin.lib.utils import is_server_ssh_ready
from marvin.cloudstackAPI import updateZone

# Import System Modules
from nose.plugins.attrib import attr
import time
import base64
import unittest
import re


class Services:
    """Test network services
    """
    def __init__(self):
        self.services = {
            "shared_network_offering": {
                "name": "MySharedOffering-shared",
                "displaytext": "MySharedOffering",
                "guestiptype": "Shared",
                "supportedservices": "Dhcp,Dns,UserData",
                "specifyVlan": "True",
                "specifyIpRanges": "True",
                "traffictype": "GUEST",
                "tags": "native",
                "serviceProviderList": {
                    "Dhcp": "VirtualRouter",
                    "Dns": "VirtualRouter",
                    "UserData": "VirtualRouter"
                }
            }
        }


class TestNuageMigration(nuageTestCase):
    """Test Native to Nuage Migration
    """

    @classmethod
    def setUpClass(cls):
        super(TestNuageMigration, cls).setUpClass()
        cls.services = Services().services

        if not hasattr(cls.vsp_physical_network, "tags") \
                or cls.vsp_physical_network.tags != 'nuage':
            raise unittest.SkipTest("Require migrateACS configuration - skip")

        # create a native vpc offering
        cls.native_vpc_offering = cls.create_VpcOffering(cls.test_data
                                                         ["vpc_offering"])
        # create a nuage vpc offering
        cls.nuage_vpc_offering = \
            cls.create_VpcOffering(cls.test_data["nuagevsp"]["vpc_offering"])

        # tier network offerings
        cls.nuage_vpc_network_offering = \
            cls.create_NetworkOffering(cls.test_data["nuagevsp"]
                                       ["vpc_network_offering"])
        cls.native_vpc_network_offering = \
            cls.create_NetworkOffering(cls.test_data
                                       ["nw_offering_isolated_vpc"])

        # create a Nuage isolated network offering with vr
        cls.nuage_isolated_network_offering = cls.create_NetworkOffering(
            cls.test_data["nuagevsp"]["isolated_network_offering"], True)

        # create a Nuage isolated network offering with vr and persistent
        cls.nuage_isolated_network_offering_persistent = \
            cls.create_NetworkOffering(
                cls.test_data["nuagevsp"]
                ["isolated_network_offering_persistent"],
                True)

        # create a Nuage isolated network offering without vr
        cls.nuage_isolated_network_offering_without_vr = \
            cls.create_NetworkOffering(
                cls.test_data["nuagevsp"]
                ["isolated_network_offering_without_vr"],
                True)

        # create a Nuage isolated network offering without vr but persistent
        cls.nuage_isolated_network_offering_without_vr_persistent = \
            cls.create_NetworkOffering(
                cls.test_data["nuagevsp"]
                ["isolated_network_offering_without_vr_persistent"],
                True)

        # create a native isolated network offering
        cls.native_isolated_network_offering = cls.create_NetworkOffering(
            cls.test_data["isolated_network_offering"], True)

        # create a native persistent isolated network offering
        cls.native_isolated_network_offering_persistent = \
            cls.create_NetworkOffering(
                cls.test_data["nw_off_isolated_persistent"], True)

        # create a native persistent staticNat isolated network offering
        cls.native_isolated_network_staticnat_offering_persistent = \
            cls.create_NetworkOffering(
                cls.test_data["isolated_staticnat_network_offering"], True)

        # create a Native shared network offering
        cls.native_shared_network_offering = cls.create_NetworkOffering(
            cls.services["shared_network_offering"], False)

        # create a Nuage shared network offering
        cls.nuage_shared_network_offering = cls.create_NetworkOffering(
            cls.test_data["nuagevsp"]["shared_nuage_network_offering"],
            False)

        cls._cleanup = [
            cls.nuage_isolated_network_offering,
            cls.nuage_isolated_network_offering_persistent,
            cls.nuage_isolated_network_offering_without_vr,
            cls.nuage_isolated_network_offering_without_vr_persistent,
            cls.native_isolated_network_offering,
            cls.native_isolated_network_offering_persistent,
            cls.native_vpc_offering,
            cls.nuage_vpc_offering,
            cls.nuage_vpc_network_offering,
            cls.native_vpc_network_offering,
            cls.native_shared_network_offering,
            cls.nuage_shared_network_offering
        ]
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.cleanup = [self.account]
        return

    def migrate_network(self, nw_off, network, resume=False):
        return network.migrate(self.api_client, nw_off.id, resume)

    def migrate_vpc(self, vpc, vpc_offering,
                    network_offering_map, resume=False):
        return vpc.migrate(self.api_client,
                           vpc_offering.id,
                           network_offering_map, resume)

    def verify_pingtovmipaddress(self, ssh, pingtovmipaddress):
        """verify ping to ipaddress of the vm and retry 3 times"""
        successfull_ping = False
        nbr_retries = 0
        max_retries = 5
        cmd = 'ping -c 2 ' + pingtovmipaddress

        while not successfull_ping and nbr_retries < max_retries:
            self.debug("ping vm by ipaddress with command: " + cmd)
            outputlist = ssh.execute(cmd)
            self.debug("command is executed properly " + cmd)
            completeoutput = str(outputlist).strip('[]')
            self.debug("complete output is " + completeoutput)
            if '2 received' in completeoutput:
                self.debug("PASS as vm is pingeable: " + completeoutput)
                successfull_ping = True
            else:
                self.debug("FAIL as vm is not pingeable: " + completeoutput)
                time.sleep(3)
                nbr_retries = nbr_retries + 1

        if not successfull_ping:
            self.fail("FAILED TEST as excepted value not found in vm")

    def verify_pingtovmhostname(self, ssh, pingtovmhostname):
        """verify ping to hostname of the vm and retry 3 times"""
        successfull_ping = False
        nbr_retries = 0
        max_retries = 5
        cmd = 'ping -c 2 ' + pingtovmhostname

        while not successfull_ping and nbr_retries < max_retries:
            self.debug("ping vm by hostname with command: " + cmd)
            outputlist = ssh.execute(cmd)
            self.debug("command is executed properly " + cmd)
            completeoutput = str(outputlist).strip('[]')
            self.debug("complete output is " + completeoutput)
            if '2 received' in completeoutput:
                self.debug("PASS as vm is pingeable: " + completeoutput)
                successfull_ping = True
            else:
                self.debug("FAIL as vm is not pingeable: " + completeoutput)
                time.sleep(3)
                nbr_retries = nbr_retries + 1

        if not successfull_ping:
            self.fail("FAILED TEST as excepted value not found in vm")

    def update_userdata(self, vm, expected_user_data):
        updated_user_data = base64.b64encode(expected_user_data)
        vm.update(self.api_client, userdata=updated_user_data)
        return expected_user_data

    def get_userdata_url(self, vm):
        self.debug("Getting user data url")
        nic = vm.nic[0]
        gateway = str(nic.gateway)
        self.debug("Gateway: " + gateway)
        user_data_url = 'curl "http://' + gateway + ':80/latest/user-data"'
        return user_data_url

    def define_cloudstack_managementip(self):
        # get cloudstack managementips from cfg file
        config = self.getClsConfig()
        return [config.mgtSvr[0].mgtSvrIp, config.mgtSvr[1].mgtSvrIp]

    def cloudstack_connection_vsd(self, connection="up",
                                  cscip=["csc-1", "csc-2"]):
        self.debug("SSH into cloudstack management server(s), setting "
                   "connection to VSD as %s " % connection)
        try:
            for ip in cscip:
                csc_ssh_client = is_server_ssh_ready(
                        ipaddress=ip,
                        port=22,
                        username="root",
                        password="tigris",
                        retries=2
                )
                self.debug("SSH is successful for cloudstack management "
                           "server with IP %s" % ip)
                if connection == "down":
                    cmd = "iptables -A OUTPUT -p tcp --dport 8443 -j DROP"
                else:
                    cmd = "iptables -D OUTPUT -p tcp --dport 8443 -j DROP"
                self.execute_cmd(csc_ssh_client, cmd)
        except Exception as e:
            self.debug("Setting cloudstack management server(s) connection %s "
                       "to VSD fails with exception %s" % (connection, e))

    def verify_cloudstack_host_state_up(self, state):
        nbr_retries = 0
        max_retries = 30
        self.debug("Verify state by list hosts type=L2Networking")
        result = Host.list(self.api_client, type="L2Networking")
        while result[0].state != state and nbr_retries < max_retries:
            time.sleep(5)
            result = Host.list(self.api_client, type="L2Networking")
            nbr_retries = nbr_retries + 1

        if nbr_retries == max_retries:
            self.debug("TIMEOUT - state of list hosts unchanged")

    def verifymigrationerrortext(self, errortext, expectstr):
        if expectstr in errortext:
            self.debug("Migrate_network fails with expected errortext %s",
                       errortext)
        else:
            self.fail("Migrate_network fails but test expects "
                      "other errortext %s", expectstr)

    @attr(tags=["migrateACS", "novms"],
          required_hardware="false")
    def test_01_native_to_nuage_network_migration_novms(self):
        """
        Verify Migration for an isolated network without VMs
        1. create multiple native non-persistent isolated network
        2. move to nuage non-persistent isolated network
        3. move to native persistent network, check VR state
        4. move to nuage persistent network, check VR state
        """
        isolated_network = self.create_Network(
            self.native_isolated_network_offering, gateway="10.0.0.1",
            netmask="255.255.255.0")

        isolated_network2 = self.create_Network(
            self.native_isolated_network_offering, gateway="10.1.0.1",
            netmask="255.255.255.0")

        shared_network = self.create_Network(
            self.native_shared_network_offering, gateway="10.3.0.1",
            netmask="255.255.255.0", vlan=1201)

        try:
            self.migrate_network(
                    self.nuage_shared_network_offering,
                    shared_network, resume=False)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "NetworkOfferingId can be upgraded only for the " \
                    "network of type Isolated"
        self.verifymigrationerrortext(errortext, expectstr)

        try:
            self.migrate_network(
                    self.nuage_shared_network_offering,
                    isolated_network, resume=False)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Can't upgrade from network offering"
        self.verifymigrationerrortext(errortext, expectstr)

        self.nuage_isolated_network_offering.update(self.api_client,
                                                    state="Disabled")
        self.validate_NetworkOffering(
                self.nuage_isolated_network_offering, state="Disabled")

        try:
            self.migrate_network(
                    self.nuage_isolated_network_offering,
                    isolated_network, resume=False)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to migrate network as the specified network " \
                    "offering is not enabled."
        self.verifymigrationerrortext(errortext, expectstr)

        self.nuage_isolated_network_offering.update(self.api_client,
                                                    state="Enabled")
        self.validate_NetworkOffering(
                self.nuage_isolated_network_offering, state="Enabled")

        self.migrate_network(
            self.nuage_isolated_network_offering, isolated_network,
            resume=True)

        self.verify_vsd_network_not_present(isolated_network, None)

        self.migrate_network(
            self.native_isolated_network_offering_persistent, isolated_network)
        self.verify_vsd_network_not_present(isolated_network, None)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")

        try:
            self.migrate_network(
                    self.nuage_vpc_offering, isolated_network, resume=False)
        except Exception as e:
            self.debug("Migration fails with %s" % e)

        try:
            self.migrate_network(
                    self.nuage_vpc_network_offering, isolated_network,
                    resume=False)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to migrate network as the specified network " \
                    "offering is a VPC offering"
        self.verifymigrationerrortext(errortext, expectstr)

        self.migrate_network(
             self.nuage_isolated_network_offering_persistent, isolated_network)
        self.verify_vsd_network(self.domain.id, isolated_network, None)

        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")
        self.verify_vsd_router(vr)
        with self.assertRaises(Exception):
            self.verify_vsd_network(self.domain.id, isolated_network2, None)

        self.add_resource_tag(
                self.native_isolated_network_offering_persistent.id,
                "NetworkOffering", "RelatedNetworkOffering",
                self.native_isolated_network_offering.id)
        result = self.list_resource_tag(
                self.native_isolated_network_offering_persistent.id,
                "NetworkOffering", "RelatedNetworkOffering")
        if result[0].value != self.native_isolated_network_offering.id:
            self.fail("Listed resource value does not match with stored"
                      " resource value!")
        self.delete_resource_tag(
                self.native_isolated_network_offering_persistent.id,
                "NetworkOffering")
        empty = self.list_resource_tag(
                self.native_isolated_network_offering_persistent.id,
                "NetworkOffering", "RelatedNetworkOffering")
        if empty:
            self.fail("clean up of resource values did was not successful!")

    @attr(tags=["migrateACS", "stoppedvms"],
          required_hardware="false")
    def test_02_native_to_nuage_network_migration_stoppedvms(self):
        """
        Verify Migration for an isolated network with stopped VMs
        1. create multiple native non-persistent isolated network
        2. deploy vm and stop vm
        3. move to nuage non-persistent isolated network
        4. deploy vm and stop vm
        5. move to native persistent network, check VR state
        6. deploy vm and stop vm
        7. move to nuage persistent network, check VR state
        """

        isolated_network = self.create_Network(
                self.native_isolated_network_offering, gateway="10.0.0.1",
                netmask="255.255.255.0")

        vm_1 = self.create_VM(isolated_network)
        vm_1.stop(self.api_client)

        self.migrate_network(
            self.nuage_isolated_network_offering, isolated_network)

        vm_1.delete(self.api_client, expunge=True)
        vm_2 = self.create_VM(isolated_network)
        vm_2.stop(self.api_client)

        self.verify_vsd_network(self.domain.id, isolated_network, None)

        self.migrate_network(
            self.native_isolated_network_offering_persistent, isolated_network)

        self.verify_vsd_network_not_present(isolated_network, None)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")

        vm_2.delete(self.api_client, expunge=True)
        vm_3 = self.create_VM(isolated_network)
        vm_3.stop(self.api_client)

        self.migrate_network(
            self.nuage_isolated_network_offering_persistent, isolated_network)

        self.verify_vsd_network(self.domain.id, isolated_network, None)
        vr2 = self.get_Router(isolated_network)
        self.check_Router_state(vr2, "Running")
        self.verify_vsd_router(vr2)
        self.verify_vsd_network(self.domain.id, isolated_network, None)

    @attr(tags=["migrateACS", "nonpersist"],
          required_hardware="false")
    def test_03_migrate_native_nonpersistent_network_to_nuage_traffic(self):
        """
        Verify traffic after Migration of a non-persistent isolated network
        1. create native non-persistent isolated network
        2. move to nuage non-persistent isolated network
        3. Spin VM's and verify traffic provided by Nuage
        """
        for i in range(1, 3):
            isolated_network = self.create_Network(
                    self.native_isolated_network_offering, gateway="10.1.0.1",
                    netmask="255.255.255.0", account=self.account)

            try:
                self.migrate_network(
                    self.nuage_vpc_network_offering,
                    isolated_network, resume=False)
            except Exception as e:
                errortext = re.search(".*errortext\s*:\s*u?'([^']+)'.*",
                                      e.message).group(1)
                self.debug("Migration fails with %s" % errortext)

            expectstr = "Failed to migrate network as the specified network " \
                        "offering is a VPC offering"
            self.verifymigrationerrortext(errortext, expectstr)

            self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr,
                    isolated_network, resume=True)

            self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr,
                    isolated_network, resume=False)

            self.verify_vsd_network_not_present(isolated_network, None)

            self.debug("Deploying a VM in the created Isolated network...")
            vm_1 = self.create_VM(isolated_network)
            self.validate_Network(isolated_network, state="Implemented")
            self.check_VM_state(vm_1, state="Running")
            vm_2 = self.create_VM(isolated_network)
            self.check_VM_state(vm_2, state="Running")

            # VSD verification
            self.verify_vsd_network(self.domain.id, isolated_network)
            # self.verify_vsd_router(vr_1)
            self.verify_vsd_vm(vm_1)
            self.verify_vsd_vm(vm_2)

            # Creating Static NAT rule
            self.debug("Creating Static NAT rule for the deployed VM in the "
                       "created Isolated network...")
            public_ip = self.acquire_PublicIPAddress(isolated_network)
            self.validate_PublicIPAddress(public_ip, isolated_network)
            self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
            self.validate_PublicIPAddress(
                    public_ip, isolated_network, static_nat=True, vm=vm_1)
            fw_rule = self.create_FirewallRule(
                    public_ip, self.test_data["ingress_rule"])

            # VSD verification for Static NAT functionality
            self.verify_vsd_floating_ip(isolated_network, vm_1,
                                        public_ip.ipaddress)
            self.verify_vsd_firewall_rule(fw_rule)

            # Ssh into the VM via floating ip
            vm_public_ip = public_ip.ipaddress.ipaddress
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

            self.verify_pingtovmipaddress(ssh, vm_2.ipaddress)

            vm_1.delete(self.api_client, expunge=True)
            vm_2.delete(self.api_client, expunge=True)
            isolated_network.delete(self.api_client)
            self.debug("Number of loops %s" % i)

    @attr(tags=["migrateACS", "persist"],
          required_hardware="false")
    def test_04_migrate_native_persistentnetwork_to_nuage_traffic(self):
        """
        Verify traffic after Migration of a persistent isolated network
        1. create native persistent isolated network
        2. move to nuage persistent isolated network without VR
        3. Spin VM's and verify traffic provided by Nuage
        """
        for i in range(1, 3):
            isolated_network = self.create_Network(
                self.native_isolated_network_offering_persistent,
                gateway="10.2.0.1",
                netmask="255.255.255.0", account=self.account)
            self.verify_vsd_network_not_present(isolated_network, None)
            vr = self.get_Router(isolated_network)
            self.check_Router_state(vr, "Running")

            csc_ips = self.define_cloudstack_managementip()
            self.cloudstack_connection_vsd(connection="down", cscip=csc_ips)
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.debug("Migrate_network fails if connection ACS VSD is down")
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.verify_cloudstack_host_state_up("Alert")
            try:
                self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr_persistent,
                    isolated_network, resume=False)
            except Exception as e:
                errortext = re.search(".*errortext\s*:\s*u?'([^']+)'.*",
                                      e.message).group(1)
                self.debug("Migration fails with %s" % errortext)

            expectstr = "Failed to implement network (with specified id) " \
                        "elements and resources as a part of network update"
            self.verifymigrationerrortext(errortext, expectstr)

            self.cloudstack_connection_vsd(connection="up", cscip=csc_ips)
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.debug("Migrate_network resumes if connection ACS VSD is up")
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.verify_cloudstack_host_state_up("Up")
            try:
                self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr_persistent,
                    isolated_network, resume=False)
            except Exception as e:
                errortext = \
                    re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                              e.message).group(2)
                self.debug("Migration fails with %s" % errortext)

            expectstr = "Failed to migrate network as previous migration " \
                        "left this network in transient condition. " \
                        "Specify resume as true."
            self.verifymigrationerrortext(errortext, expectstr)

            self.migrate_network(
                self.nuage_isolated_network_offering_without_vr_persistent,
                isolated_network, resume=True)

            self.verify_vsd_network(self.domain.id, isolated_network, None)
            with self.assertRaises(Exception):
                self.get_Router(isolated_network)
            self.debug("Deploying a VM in the created Isolated network...")
            vm_1 = self.create_VM(isolated_network)
            self.validate_Network(isolated_network, state="Implemented")
            with self.assertRaises(Exception):
                self.get_Router(isolated_network)
            self.check_VM_state(vm_1, state="Running")
            vm_2 = self.create_VM(isolated_network)
            self.check_VM_state(vm_2, state="Running")

            # VSD verification
            self.verify_vsd_network(self.domain.id, isolated_network)
            self.verify_vsd_vm(vm_1)
            self.verify_vsd_vm(vm_2)

            # Creating Static NAT rule
            self.debug("Creating Static NAT rule for the deployed VM in the "
                       "created Isolated network...")
            public_ip = self.acquire_PublicIPAddress(isolated_network)
            self.validate_PublicIPAddress(public_ip, isolated_network)
            self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
            self.validate_PublicIPAddress(
                    public_ip, isolated_network, static_nat=True, vm=vm_1)
            fw_rule = self.create_FirewallRule(
                    public_ip, self.test_data["ingress_rule"])

            # VSD verification for Static NAT functionality
            self.verify_vsd_floating_ip(isolated_network, vm_1,
                                        public_ip.ipaddress)
            self.verify_vsd_firewall_rule(fw_rule)

            # Ssh into the VM via floating ip
            vm_public_ip = public_ip.ipaddress.ipaddress
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

            self.verify_pingtovmipaddress(ssh, vm_2.ipaddress)

            vm_1.delete(self.api_client, expunge=True)
            vm_2.delete(self.api_client, expunge=True)
            isolated_network.delete(self.api_client)
            self.debug("Number of loops %s" % i)

    @attr(tags=["migrateACS", "nicmigration"],
          required_hardware="false")
    def test_05_native_to_nuage_nic_migration(self):
        """
        Verify Nic migration of GuestVm in an isolated network
        1. create multiple native non-persistent isolated network
        2. populate network with 2 vm's
        3. enable static nat + create FW rule
        4. move to nuage non-persistent isolated network, check:
            - public ip
            - FW rules
            - VR
            - VM's
        """
        isolated_network = self.create_Network(
            self.native_isolated_network_offering, gateway="10.0.0.1",
            netmask="255.255.255.0")

        isolated_network2 = self.create_Network(
            self.native_isolated_network_offering, gateway="10.1.0.1",
            netmask="255.255.255.0")

        vm1 = self.create_VM(isolated_network)
        public_ip = self.acquire_PublicIPAddress(isolated_network)
        self.create_StaticNatRule_For_VM(vm1, public_ip, isolated_network)
        firewall_rule = self.create_FirewallRule(public_ip)

        vm2 = self.create_VM(isolated_network)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")

        self.migrate_network(
            self.nuage_isolated_network_offering, isolated_network)

        self.verify_vsd_network(self.domain.id, isolated_network, None)
        self.verify_vsd_vm(vm1)
        self.verify_vsd_floating_ip(isolated_network, vm1, public_ip.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule)
        self.verify_vsd_vm(vm2)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")
        self.verify_vsd_router(vr)
        with self.assertRaises(Exception):
            self.verify_vsd_network(self.domain.id, isolated_network2, None)

    @attr(tags=["migrateACS", "nonpersistnic"],
          required_hardware="false")
    def test_06_migrate_native_nonpersistent_nic_to_nuage_traffic(self):
        """
        Verify Nic migration of GuestVm in a non-persistent isolated network
        1. create two native non-persistent isolated networks
        2. Deploy 2 vm's in first network
        3. enable static nat + create FW rule
        4. move first network to nuage non-persistent isolated network,
            check:
                - public ip
                - FW rules
                - VR
                - VM's
        5. Destroy and expunge 1 VM in first network
        6. Deploy a new VM in first network and check traffic
        7. Move second network to nuage non-persistent isolated network,
            check:
                - public ip
                - FW rules
                - VR
                - VM's
        8. Deploy 2 vm in second network , move it and check
        9. Cleanup
        """
        isolated_network = self.create_Network(
                self.native_isolated_network_offering, gateway="10.3.0.1",
                netmask="255.255.255.0")

        isolated_network2 = self.create_Network(
                self.native_isolated_network_offering, gateway="10.4.0.1",
                netmask="255.255.255.0")

        vm_1 = self.create_VM(isolated_network)
        public_ip = self.acquire_PublicIPAddress(isolated_network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
        firewall_rule = self.create_FirewallRule(public_ip)

        vm_2 = self.create_VM(isolated_network)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")

        self.migrate_network(
                self.nuage_isolated_network_offering, isolated_network)

        self.verify_vsd_network(self.domain.id, isolated_network, None)
        self.verify_vsd_vm(vm_1)
        self.verify_vsd_floating_ip(isolated_network, vm_1,
                                    public_ip.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule)
        self.verify_vsd_vm(vm_2)
        vr_3 = self.get_Router(isolated_network)
        self.check_Router_state(vr_3, "Running")
        self.verify_vsd_router(vr_3)

        # VSD verification for Static NAT functionality
        self.verify_vsd_floating_ip(isolated_network, vm_1,
                                    public_ip.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule)

        # Ssh into the VM via floating ip
        vm_public_ip = public_ip.ipaddress.ipaddress
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

        self.verify_pingtovmipaddress(ssh, vm_2.ipaddress)

        vm_2.delete(self.api_client, expunge=True)
        with self.assertRaises(Exception):
            self.verify_vsd_vm(vm_2)
        vm_4 = self.create_VM(isolated_network)
        self.verify_vsd_vm(vm_4)
        with self.assertRaises(Exception):
            self.verify_vsd_network(self.domain.id, isolated_network2, None)

        vm_3 = self.create_VM(isolated_network2)
        vr_2 = self.get_Router(isolated_network2)
        self.check_Router_state(vr_2, "Running")

        # Ssh into the VM via floating ip
        vm_public_ip = public_ip.ipaddress.ipaddress
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

        self.verify_pingtovmipaddress(ssh, vm_4.ipaddress)

        vm_1.delete(self.api_client, expunge=True)
        vm_4.delete(self.api_client, expunge=True)
        isolated_network.delete(self.api_client)

        self.migrate_network(
                self.nuage_isolated_network_offering_without_vr,
                isolated_network2)

        public_ip_2 = self.acquire_PublicIPAddress(isolated_network2)
        self.create_StaticNatRule_For_VM(vm_3, public_ip_2, isolated_network2)
        firewall_rule_2 = self.create_FirewallRule(public_ip_2)
        self.verify_vsd_floating_ip(isolated_network2, vm_3,
                                    public_ip_2.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule_2)

        self.verify_vsd_network(self.domain.id, isolated_network2, None)
        self.verify_vsd_vm(vm_3)
        self.verify_vsd_floating_ip(isolated_network2, vm_3,
                                    public_ip_2.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule_2)

        with self.assertRaises(Exception):
            self.get_Router(isolated_network2)
        vm_5 = self.create_VM(isolated_network2)
        self.verify_vsd_vm(vm_5)

        # Ssh into the VM via floating ip
        vm_public_ip_2 = public_ip_2.ipaddress.ipaddress
        try:
            vm_3.ssh_ip = vm_public_ip_2
            vm_3.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_3.username = self.test_data["virtual_machine"]["username"]
            vm_3.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_3.ssh_ip, vm_3.password))

            ssh = vm_3.get_ssh_client(ipaddress=vm_public_ip_2)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        self.verify_pingtovmipaddress(ssh, vm_5.ipaddress)

    @attr(tags=["migrateACS", "persistnic"],
          required_hardware="false")
    def test_07_migrate_native_persistent_nic_to_nuage_traffic(self):
        """
        Verify Nic migration of GuestVm in a persistent isolated network
        1. create two native persistent isolated networks
        2. deploy 2 vm's in this network
        3. move to nuage non-persistent isolated network,
        4. enable static nat + create FW rule
        check:
            - public ip
            - FW rules
            - VR
            - VM's
        """
        isolated_network = self.create_Network(
                self.native_isolated_network_offering_persistent,
                gateway="10.5.0.1",
                netmask="255.255.255.0")

        isolated_network2 = self.create_Network(
                self.native_isolated_network_offering_persistent,
                gateway="10.6.0.1",
                netmask="255.255.255.0")

        vm_1 = self.create_VM(isolated_network)
        vm_2 = self.create_VM(isolated_network)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")
        vm_3 = self.create_VM(isolated_network2)

        self.migrate_network(
                self.nuage_isolated_network_offering_without_vr_persistent,
                isolated_network)

        public_ip = self.acquire_PublicIPAddress(isolated_network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
        firewall_rule = self.create_FirewallRule(public_ip)

        self.verify_vsd_network(self.domain.id, isolated_network, None)
        self.verify_vsd_vm(vm_1)
        self.verify_vsd_floating_ip(isolated_network, vm_1,
                                    public_ip.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule)
        self.verify_vsd_vm(vm_2)
        with self.assertRaises(Exception):
            self.get_Router(isolated_network)

        # Ssh into the VM via floating ip
        vm_public_ip = public_ip.ipaddress.ipaddress
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

        self.verify_pingtovmipaddress(ssh, vm_2.ipaddress)

        vm_1.delete(self.api_client, expunge=True)

        self.migrate_network(
                self.nuage_isolated_network_offering_without_vr_persistent,
                isolated_network2)

        vm_2.delete(self.api_client, expunge=True)
        isolated_network.delete(self.api_client)

        self.verify_vsd_network(self.domain.id, isolated_network2, None)
        self.verify_vsd_vm(vm_3)
        public_ip_2 = self.acquire_PublicIPAddress(isolated_network2)
        self.create_StaticNatRule_For_VM(vm_3, public_ip_2, isolated_network2)
        firewall_rule_2 = self.create_FirewallRule(public_ip_2)
        vm_4 = self.create_VM(isolated_network2)
        self.verify_vsd_floating_ip(isolated_network2, vm_3,
                                    public_ip_2.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule_2)
        self.verify_vsd_vm(vm_4)

        # Ssh into the VM via floating ip
        vm_public_ip_2 = public_ip_2.ipaddress.ipaddress
        try:
            vm_3.ssh_ip = vm_public_ip_2
            vm_3.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_3.username = self.test_data["virtual_machine"]["username"]
            vm_3.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_3.ssh_ip, vm_3.password))

            ssh = vm_3.get_ssh_client(ipaddress=vm_public_ip_2)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        self.verify_pingtovmipaddress(ssh, vm_4.ipaddress)

    @attr(tags=["migrateACS", "isomultinic"],
          required_hardware="false")
    def test_08_migrate_native_multinic_to_nuage_traffic(self):
        """
        Verify MultiNic migration of GuestVm with multiple isolated networks
        1. create one native non-persistent isolated network
        2. create one native persistent isolated network
        3. deploy 2 vm's in both these network
        4. move non-persist to nuage non-persistent isolated network,
        5. move persist to nuage persistent isolated network
        6. enable static nat + create FW rule
        check:
            - public ip
            - FW rules
            - VR
            - VM's
        """
        isolated_network = self.create_Network(
                self.native_isolated_network_offering,
                gateway="10.7.0.1",
                netmask="255.255.255.0")

        isolated_network2 = self.create_Network(
                self.native_isolated_network_offering_persistent,
                gateway="10.8.0.1",
                netmask="255.255.255.0")

        vm_1 = self.create_VM([isolated_network, isolated_network2])
        vm_2 = self.create_VM([isolated_network2, isolated_network])
        vr = self.get_Router(isolated_network2)
        self.check_Router_state(vr, "Running")

        self.migrate_network(
                self.nuage_isolated_network_offering_without_vr,
                isolated_network)

        vm_3 = self.create_VM(isolated_network)

        public_ip = self.acquire_PublicIPAddress(isolated_network)
        self.create_StaticNatRule_For_VM(vm_3, public_ip, isolated_network)
        firewall_rule = self.create_FirewallRule(public_ip)

        self.verify_vsd_network(self.domain.id, isolated_network, None)

        self.verify_vsd_vm(vm_3)
        self.verify_vsd_floating_ip(isolated_network, vm_3,
                                    public_ip.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule)

        # Ssh into the VM via floating ip
        vm_public_ip = public_ip.ipaddress.ipaddress
        try:
            vm_3.ssh_ip = vm_public_ip
            vm_3.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_3.username = self.test_data["virtual_machine"]["username"]
            vm_3.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_3.ssh_ip, vm_3.password))

            ssh = vm_3.get_ssh_client(ipaddress=vm_public_ip)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        defaultipaddress = \
            [nic.ipaddress for nic in vm_1.nic if nic.isdefault][0]

        self.verify_pingtovmipaddress(ssh, defaultipaddress)

        self.migrate_network(
                self.nuage_isolated_network_offering_without_vr_persistent,
                isolated_network2)

        vm_3.delete(self.api_client, expunge=True)
        public_ip.delete(self.api_client)
        vm_4 = self.create_VM(isolated_network2)

        public_ip_2 = self.acquire_PublicIPAddress(isolated_network2)
        self.create_StaticNatRule_For_VM(vm_4, public_ip_2, isolated_network2)
        firewall_rule_2 = self.create_FirewallRule(public_ip_2)

        self.verify_vsd_floating_ip(isolated_network2, vm_4,
                                    public_ip_2.ipaddress)
        self.verify_vsd_firewall_rule(firewall_rule_2)
        self.verify_vsd_vm(vm_4)

        # Ssh into the VM via floating ip
        vm_public_ip_2 = public_ip_2.ipaddress.ipaddress
        try:
            vm_4.ssh_ip = vm_public_ip_2
            vm_4.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_4.username = self.test_data["virtual_machine"]["username"]
            vm_4.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_4.ssh_ip, vm_4.password))

            ssh = vm_4.get_ssh_client(ipaddress=vm_public_ip_2)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        defaultipaddress2 = \
            [nic.ipaddress for nic in vm_2.nic if nic.isdefault][0]

        self.verify_pingtovmipaddress(ssh, defaultipaddress2)

    @attr(tags=["migrateACS", "persiststaticnat"],
          required_hardware="false")
    def test_09_migrate_native_persist_staticnat_to_nuage_traffic(self):
        """
        Verify StaticNat migration of GuestVm in a persistent isolated network
        1. create one native persistent isolated network offering
        2. with Dhcp,SourceNat,StaticNat,Dns,Userdata and Firewall
        3. create one native persistent isolated network with above
        4. deploy 2 vm's in this network
        5. In a loop
              enable staticnat on first vm and open port 22 for ssh
              login to vm1 and ping vm2
              verify userdata in Native
              move to nuage persistent isolated network,
              deploy 2 new vm's in this network
              enable staticnat on new vm and open port 22 for ssh
              check:
               - public ips
               - FW rules
               - VR
               - VM's ping old and new VM's
               - verify userdata in Nuage
               - Release public ips
              move back to native persistent isolated network,
        """
        isolated_network = self.create_Network(
                self.native_isolated_network_staticnat_offering_persistent,
                gateway="10.9.0.1",
                netmask="255.255.255.0", account=self.account)

        vm_1 = self.create_VM(isolated_network)
        vm_2 = self.create_VM(isolated_network)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")

        for i in range(1, 3):
            self.debug("+++++Starting again as Native in Loop+++++")
            public_ip = self.acquire_PublicIPAddress(isolated_network)
            self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
            firewall_rule = self.create_FirewallRule(public_ip)

            self.migrate_network(
                    self.nuage_isolated_network_offering_persistent,
                    isolated_network)

            self.verify_vsd_network(self.domain.id, isolated_network, None)
            self.verify_vsd_vm(vm_1)
            self.verify_vsd_floating_ip(isolated_network, vm_1,
                                        public_ip.ipaddress)
            self.verify_vsd_firewall_rule(firewall_rule)
            self.verify_vsd_vm(vm_2)

            vm_3 = self.create_VM(isolated_network)
            public_ip_2 = self.acquire_PublicIPAddress(isolated_network)
            self.create_StaticNatRule_For_VM(vm_3,
                                             public_ip_2,
                                             isolated_network)
            firewall_rule_2 = self.create_FirewallRule(public_ip_2)
            vm_4 = self.create_VM(isolated_network)
            self.verify_vsd_floating_ip(isolated_network, vm_3,
                                        public_ip_2.ipaddress)
            self.verify_vsd_firewall_rule(firewall_rule_2)
            self.verify_vsd_vm(vm_3)
            self.verify_vsd_vm(vm_4)

            # Ssh into the VM via floating ip
            vm_public_ip_2 = public_ip_2.ipaddress.ipaddress
            try:
                vm_3.ssh_ip = vm_public_ip_2
                vm_3.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
                vm_3.username = self.test_data["virtual_machine"]["username"]
                vm_3.password = self.test_data["virtual_machine"]["password"]
                self.debug("SSHing into VM: %s with %s" %
                           (vm_3.ssh_ip, vm_3.password))

                ssh2 = vm_3.get_ssh_client(ipaddress=vm_public_ip_2)

            except Exception as e:
                self.fail("SSH into VM failed with exception %s" % e)

            self.verify_pingtovmipaddress(ssh2, vm_1.ipaddress)

            self.verify_pingtovmipaddress(ssh2, vm_4.ipaddress)

            self.verify_pingtovmipaddress(ssh2, vm_2.ipaddress)

            self.debug("Updating the running vm_3 with new user data...")
            expected_user_data2 = self.update_userdata(vm_3, "hellonuage vm3")
            self.debug("SSHing into the vm_3 for verifying its user data...")
            user_data_cmd = self.get_userdata_url(vm_3)
            self.debug("Getting user data with command: " + user_data_cmd)
            actual_user_data2 = self.execute_cmd(ssh2, user_data_cmd)
            self.debug("Actual user data - " + actual_user_data2 +
                       ", Expected user data - " + expected_user_data2)
            self.assertEqual(actual_user_data2, expected_user_data2,
                             "Un-expected VM (VM_3) user data")

            vm_3.delete(self.api_client, expunge=True)
            vm_4.delete(self.api_client, expunge=True)
            public_ip_2.delete(self.api_client)

            # ReleaseIP as get_ssh_client fails when migrating back to native
            public_ip.delete(self.api_client)

            self.debug("++++++++Migrating network back to native+++++++")
            self.migrate_network(
                    self.native_isolated_network_staticnat_offering_persistent,
                    isolated_network)

    @attr(tags=["migrateACS", "vpcnovms"],
          required_hardware="false")
    def test_10_migrate_native_vpc(self):
        vpc = self.create_Vpc(self.native_vpc_offering)
        network = self.create_Network(self.native_vpc_network_offering,
                                      vpc=vpc)
        self.create_VM(network)

        network_offering_map = \
            [{"networkid": network.id,
              "networkofferingid":
              self.nuage_isolated_network_offering_without_vr.id}]

        try:
            self.migrate_vpc(vpc, self.nuage_vpc_offering,
                             network_offering_map, resume=False)
        except Exception as e:
            errortext = re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                                  e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "can't be used for VPC networks"
        self.verifymigrationerrortext(errortext, expectstr)

        network_offering_map = \
            [{"networkid": network.id,
              "networkofferingid": self.nuage_vpc_network_offering.id}]
        self.migrate_vpc(vpc, self.nuage_vpc_offering,
                         network_offering_map)
        self.verify_vsd_network(self.domain.id, network, vpc)

    @attr(tags=["migrateACS", "vpcstaticnat"],
          required_hardware="false")
    def test_11_migrate_native_vpc_staticnat_to_nuage_traffic(self):
        """
        Verify StaticNat migration of GuestVm in a vpc network
        1. create one native vpc network offering
        2. create one native vpc tier network offering
        3. with Dhcp,SourceNat,StaticNat,Dns,Userdata and NetworkACL
        4. create one vpc with above native vpc network offering
        5. create one native vpc tier network in above vpc
        6. deploy 2 vm's in this tier network
        7. In a loop
              enable staticnat on first vm and ssh into vm
              login to vm1 and ping vm2
              verify userdata in Native
              move to nuage vpc tier networkoffering,
              deploy 2 new vm's in this network
              enable staticnat on new vm and ssh into vm
              check:
               - public ips
               - NetworkACL rules
               - VR
               - VM's ping old and new VM's
               - verify userdata in Nuage
               - Release public ips
              move back to native vpc tier network offering,
        """
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.api_client.updateZone(cmd)

        self.debug("Creating Native VSP VPC offering with Static NAT service "
                   "provider as VPCVR...")
        native_vpc_off = self.create_VpcOffering(
                self.test_data["vpc_offering_reduced"])
        self.validate_VpcOffering(native_vpc_off, state="Enabled")

        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter")
        vpc = self.create_Vpc(native_vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        self.debug("Creating native VPC Network Tier offering "
                   "with Static NAT service provider as VPCVR")
        native_tiernet_off = self.create_NetworkOffering(
                self.test_data["nw_offering_reduced_vpc"])
        self.validate_NetworkOffering(native_tiernet_off, state="Enabled")

        acl_list = self.create_NetworkAclList(
                name="acl", description="acl", vpc=vpc)

        acl_item = self.create_NetworkAclRule(
                self.test_data["ingress_rule"], acl_list=acl_list)
        self.create_NetworkAclRule(
                self.test_data["icmprule"], acl_list=acl_list)

        self.debug("Creating a VPC tier network with Static NAT service")
        vpc_tier = self.create_Network(native_tiernet_off,
                                       gateway='10.1.0.1',
                                       vpc=vpc,
                                       acl_list=acl_list)
        self.validate_Network(vpc_tier, state="Implemented")

        self.debug("Creating 2nd VPC tier network with Static NAT service")
        vpc_2ndtier = self.create_Network(native_tiernet_off,
                                          gateway='10.1.128.1',
                                          vpc=vpc,
                                          acl_list=acl_list)
        self.validate_Network(vpc_2ndtier, state="Implemented")

        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        self.debug("Deploying a VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm1"
        self.test_data["virtual_machine"]["name"] = "vpcvm1"
        vpc_vm_1 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_1, state="Running")

        self.debug("Deploying another VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm2"
        self.test_data["virtual_machine"]["name"] = "vpcvm2"
        vpc_vm_2 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_2, state="Running")

        self.debug("Deploying a VM in the 2nd VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm12"
        self.test_data["virtual_machine"]["name"] = "vpcvm12"
        vpc_vm_12 = self.create_VM(vpc_2ndtier)
        self.check_VM_state(vpc_vm_2, state="Running")
        self.test_data["virtual_machine"]["displayname"] = None
        self.test_data["virtual_machine"]["name"] = None

        for i in range(1, 3):
            self.debug("+++++Starting again as Native in Loop+++++")
            self.debug("Creating Static NAT rule for the deployed VM "
                       "in the created VPC network...")
            public_ip_1 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
            self.validate_PublicIPAddress(public_ip_1, vpc_tier)
            self.create_StaticNatRule_For_VM(vpc_vm_1, public_ip_1, vpc_tier)
            self.validate_PublicIPAddress(
                    public_ip_1, vpc_tier, static_nat=True, vm=vpc_vm_1)

            network_offering_map = \
                [{"networkid": vpc_tier.id,
                  "networkofferingid": self.nuage_vpc_network_offering.id},
                 {"networkid": vpc_2ndtier.id,
                  "networkofferingid": self.nuage_vpc_network_offering.id}]

            csc_ips = self.define_cloudstack_managementip()
            self.cloudstack_connection_vsd(connection="down",
                                           cscip=csc_ips)
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.debug("Migrate_vpc fails when connection ACS VSD is down")
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.verify_cloudstack_host_state_up("Alert")
            try:
                self.migrate_vpc(vpc, self.nuage_vpc_offering,
                                 network_offering_map, resume=False)
            except Exception as e:
                errortext = \
                    re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                              e.message).group(2)
                self.debug("Migration fails with %s" % errortext)

            expectstr = "Failed to implement network (with specified id) " \
                        "elements and resources as a part of network update"
            self.verifymigrationerrortext(errortext, expectstr)

            self.cloudstack_connection_vsd(connection="up",
                                           cscip=csc_ips)
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.debug("Migrate_vpc resumes when connection ACS VSD is up")
            self.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
            self.verify_cloudstack_host_state_up("Up")
            try:
                self.migrate_vpc(vpc, self.nuage_vpc_offering,
                                 network_offering_map, resume=False)
            except Exception as e:
                errortext = \
                    re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                              e.message).group(2)
                self.debug("Migration fails with %s" % errortext)

            expectstr = "Failed to migrate VPC as previous migration " \
                        "left this VPC in transient condition. " \
                        "Specify resume as true."
            self.verifymigrationerrortext(errortext, expectstr)

            network_offering_map = \
                [{"networkid": vpc_tier.id,
                  "networkofferingid": self.nuage_vpc_network_offering.id},
                 {"networkid": vpc_2ndtier.id,
                  "networkofferingid":
                      self.nuage_isolated_network_offering.id}]
            try:
                self.migrate_vpc(vpc, self.nuage_vpc_offering,
                                 network_offering_map, resume=True)
            except Exception as e:
                errortext = \
                    re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                              e.message).group(2)
                self.debug("Migration fails with %s" % errortext)

            expectstr = "can't be used for VPC networks for network"
            self.verifymigrationerrortext(errortext, expectstr)

            network_offering_map = \
                [{"networkid": vpc_tier.id,
                  "networkofferingid": self.nuage_vpc_network_offering.id},
                 {"networkid": vpc_2ndtier.id,
                  "networkofferingid": self.nuage_vpc_network_offering.id}]
            self.migrate_vpc(vpc, self.nuage_vpc_offering,
                             network_offering_map, resume=True)
            # checking after successful migrate vpc, migrate still succeeds
            self.migrate_vpc(vpc, self.nuage_vpc_offering,
                             network_offering_map, resume=True)
            self.migrate_vpc(vpc, self.nuage_vpc_offering,
                             network_offering_map, resume=False)

            # VSD verification after VPC migration functionality
            self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
            self.verify_vsd_vm(vpc_vm_1)
            self.verify_vsd_vm(vpc_vm_2)
            self.verify_vsd_floating_ip(
                    vpc_tier, vpc_vm_1, public_ip_1.ipaddress, vpc=vpc)
            self.verify_vsd_firewall_rule(acl_item)
            # self.verify_vsd_firewall_rule(acl_item2)
            self.verify_vsd_vm(vpc_vm_12)

            self.test_data["virtual_machine"]["displayname"] = "vpcvm3"
            self.test_data["virtual_machine"]["name"] = "vpcvm3"
            vpc_vm_3 = self.create_VM(vpc_tier)

            public_ip_2 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
            self.create_StaticNatRule_For_VM(vpc_vm_3,
                                             public_ip_2,
                                             vpc_tier)

            self.test_data["virtual_machine"]["displayname"] = "vpcvm4"
            self.test_data["virtual_machine"]["name"] = "vpcvm4"
            vpc_vm_4 = self.create_VM(vpc_tier)
            self.verify_vsd_floating_ip(vpc_tier, vpc_vm_3,
                                        public_ip_2.ipaddress, vpc=vpc)
            self.verify_vsd_vm(vpc_vm_3)
            self.verify_vsd_vm(vpc_vm_4)
            self.test_data["virtual_machine"]["displayname"] = None
            self.test_data["virtual_machine"]["name"] = None

            vm_public_ip_2 = public_ip_2.ipaddress.ipaddress
            try:
                vpc_vm_3.ssh_ip = vm_public_ip_2
                vpc_vm_3.ssh_port = \
                    self.test_data["virtual_machine"]["ssh_port"]
                vpc_vm_3.username = \
                    self.test_data["virtual_machine"]["username"]
                vpc_vm_3.password = \
                    self.test_data["virtual_machine"]["password"]
                self.debug("SSHing into VM: %s with %s" %
                           (vpc_vm_3.ssh_ip, vpc_vm_3.password))

                ssh2 = vpc_vm_3.get_ssh_client(ipaddress=vm_public_ip_2)

            except Exception as e:
                self.fail("SSH into VM failed with exception %s" % e)

            self.verify_pingtovmipaddress(ssh2, vpc_vm_1.ipaddress)
            self.verify_pingtovmhostname(ssh2, "vpcvm1")

            self.verify_pingtovmipaddress(ssh2, vpc_vm_4.ipaddress)
            self.verify_pingtovmhostname(ssh2, "vpcvm4")

            self.verify_pingtovmipaddress(ssh2, vpc_vm_2.ipaddress)
            self.verify_pingtovmhostname(ssh2, "vpcvm2")

            self.debug("Updating the running vm_3 with new user data...")
            expected_user_data2 = self.update_userdata(vpc_vm_3,
                                                       "hellonuage vm3")
            self.debug("SSHing into the vm_3 for verifying its user data...")
            user_data_cmd = self.get_userdata_url(vpc_vm_3)
            self.debug("Getting user data with command: " + user_data_cmd)
            actual_user_data2 = self.execute_cmd(ssh2, user_data_cmd)
            self.debug("Actual user data - " + actual_user_data2 +
                       ", Expected user data - " + expected_user_data2)
            self.assertEqual(actual_user_data2, expected_user_data2,
                             "Un-expected VM (VM_3) user data")

            self.verify_pingtovmipaddress(ssh2, vpc_vm_12.ipaddress)
            self.verify_pingtovmhostname(ssh2, "vpcvm12")

            vpc_vm_3.delete(self.api_client, expunge=True)
            vpc_vm_4.delete(self.api_client, expunge=True)
            public_ip_2.delete(self.api_client)

            # ReleaseIP as get_ssh_client fails when migrating back to native
            public_ip_1.delete(self.api_client)

            self.debug("++++++++Migrating network back to native+++++++")
            network_offering_map = \
                [{"networkid": vpc_tier.id,
                  "networkofferingid": native_tiernet_off.id},
                 {"networkid": vpc_2ndtier.id,
                  "networkofferingid": native_tiernet_off.id}]

            self.migrate_vpc(vpc, native_vpc_off,
                             network_offering_map)

    @attr(tags=["migrateACS", "vpcmultinic"],
          required_hardware="false")
    def test_12_migrate_native_vpc_multinic_to_nuage_traffic(self):
        """
        Verify MultiNic migration of GuestVm in multiple networks
        1. create one native vpc network offering
        2. create one native vpc tier network offering
        3. with Dhcp,SourceNat,StaticNat,Dns,Userdata and NetworkACL
        4. create one vpc with above native vpc network offering
        5. create one native vpc tier network in above vpc
        6. create 2nd isolated network
        7. deploy 2 vm's  in both networks
        8. move to nuage vpc tier networkoffering,
        9. deploy vm3 in vpc tier network
        10. enable staticnat on first vm and ssh into vm
        11. login to vm3 and ping vm1 nic1
        12. move isolated network to nuage networkoffering,
        13. deploy vm4 in 2nd vpc tier network
        14. enable staticnat on new vm4 and ssh into vm
        15. login to vm4 and ping vm2 nic1
        """
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.api_client.updateZone(cmd)

        self.debug("Creating Native VSP VPC offering with Static NAT service "
                   "provider as VPCVR...")
        native_vpc_off = self.create_VpcOffering(
                self.test_data["vpc_offering_reduced"])
        self.validate_VpcOffering(native_vpc_off, state="Enabled")

        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter")
        vpc = self.create_Vpc(native_vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        self.debug("Creating native VPC Network Tier offering "
                   "with Static NAT service provider as VPCVR")
        native_tiernet_off = self.create_NetworkOffering(
                self.test_data["nw_offering_reduced_vpc"])
        self.validate_NetworkOffering(native_tiernet_off, state="Enabled")

        acl_list = self.create_NetworkAclList(
                name="acl", description="acl", vpc=vpc)

        acl_item = self.create_NetworkAclRule(
                self.test_data["ingress_rule"], acl_list=acl_list)
        self.create_NetworkAclRule(
                self.test_data["icmprule"], acl_list=acl_list)

        self.debug("Creating a VPC tier network with Static NAT service")
        vpc_tier = self.create_Network(native_tiernet_off,
                                       gateway='10.1.0.1',
                                       vpc=vpc,
                                       acl_list=acl_list)
        self.validate_Network(vpc_tier, state="Implemented")

        self.debug("Creating 2nd VPC tier network with Static NAT service")
        vpc_2ndtier = self.create_Network(native_tiernet_off,
                                          gateway='10.1.128.1',
                                          vpc=vpc,
                                          acl_list=acl_list)
        self.validate_Network(vpc_2ndtier, state="Implemented")

        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        isolated_network = self.create_Network(
                self.native_isolated_network_staticnat_offering_persistent,
                gateway="10.10.0.1",
                netmask="255.255.255.0", account=self.account)
        self.debug("Creating isolated network with Static NAT service")

        self.debug("Deploying a multinic VM in both networks")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm1"
        self.test_data["virtual_machine"]["name"] = "vpcvm1"
        vpc_vm_1 = self.create_VM([vpc_tier, isolated_network])
        self.check_VM_state(vpc_vm_1, state="Running")

        self.debug("Deploying another VM in both networks other defaultnic")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm2"
        self.test_data["virtual_machine"]["name"] = "vpcvm2"
        vpc_vm_2 = self.create_VM([isolated_network, vpc_tier])
        self.check_VM_state(vpc_vm_2, state="Running")
        self.test_data["virtual_machine"]["displayname"] = None
        self.test_data["virtual_machine"]["name"] = None
        network_offering_map_fault = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.nuage_vpc_network_offering.id}]

        try:
            self.migrate_vpc(vpc, self.nuage_vpc_offering,
                             network_offering_map_fault, resume=False)
        except Exception as e:
            errortext = re.search(".*errortext\s*:\s*u?'([^']+)'.*",
                                  e.message).group(1)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to migrate VPC as the specified " \
                    "tierNetworkOfferings is not complete"
        self.verifymigrationerrortext(errortext, expectstr)

        try:
            self.migrate_network(self.nuage_isolated_network_offering,
                                 vpc_tier, resume=False)
        except Exception as e:
            errortext = re.search(".*errortext\s*:\s*u?'([^']+)'.*",
                                  e.message).group(1)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to migrate network as the specified " \
                    "network is a vpc tier. Use migrateVpc."
        self.verifymigrationerrortext(errortext, expectstr)

        network_offering_map_fault2 = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.nuage_vpc_network_offering.id},
             {"networkid": vpc_2ndtier.id,
              "networkofferingid": self.nuage_isolated_network_offering.id}]

        try:
            self.migrate_vpc(vpc, self.nuage_vpc_offering,
                             network_offering_map_fault2, resume=True)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "can't be used for VPC networks for network"
        self.verifymigrationerrortext(errortext, expectstr)

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.nuage_vpc_network_offering.id},
             {"networkid": vpc_2ndtier.id,
              "networkofferingid": self.nuage_vpc_network_offering.id}]

        self.migrate_vpc(vpc, self.nuage_vpc_offering,
                         network_offering_map, resume=True)

        self.debug("Deploying another VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm3"
        self.test_data["virtual_machine"]["name"] = "vpcvm3"
        vpc_vm_3 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_3, state="Running")
        self.test_data["virtual_machine"]["displayname"] = None
        self.test_data["virtual_machine"]["name"] = None
        self.debug("Creating Static NAT rule for the deployed VM "
                   "in the created VPC network...")
        public_ip_1 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_1, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm_3, public_ip_1, vpc_tier)
        self.validate_PublicIPAddress(
                public_ip_1, vpc_tier, static_nat=True, vm=vpc_vm_3)

        # VSD verification after VPC migration functionality
        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_vm(vpc_vm_3)
        self.verify_vsd_floating_ip(
            vpc_tier, vpc_vm_3, public_ip_1.ipaddress, vpc=vpc)
        self.verify_vsd_firewall_rule(acl_item)
        # self.verify_vsd_vm(vpc_vm_1)

        vm_public_ip_1 = public_ip_1.ipaddress.ipaddress
        try:
            vpc_vm_3.ssh_ip = vm_public_ip_1
            vpc_vm_3.ssh_port = \
                self.test_data["virtual_machine"]["ssh_port"]
            vpc_vm_3.username = \
                self.test_data["virtual_machine"]["username"]
            vpc_vm_3.password = \
                self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vpc_vm_3.ssh_ip, vpc_vm_1.password))

            ssh = vpc_vm_3.get_ssh_client(ipaddress=vm_public_ip_1)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        defaultipaddress = \
            [nic.ipaddress for nic in vpc_vm_1.nic if nic.isdefault][0]

        self.verify_pingtovmipaddress(ssh, defaultipaddress)

        vpc_vm_3.delete(self.api_client, expunge=True)
        public_ip_1.delete(self.api_client)

        self.migrate_network(
                self.nuage_isolated_network_offering_without_vr,
                isolated_network)

        self.test_data["virtual_machine"]["displayname"] = "vm4"
        self.test_data["virtual_machine"]["name"] = "vm4"
        vm_4 = self.create_VM(isolated_network)
        self.test_data["virtual_machine"]["displayname"] = None
        self.test_data["virtual_machine"]["name"] = None
        self.debug("Creating Static NAT rule for the deployed VM "
                   "in the created VPC network...")
        public_ip_2 = self.acquire_PublicIPAddress(isolated_network)
        self.validate_PublicIPAddress(public_ip_1, isolated_network)
        self.create_StaticNatRule_For_VM(vm_4, public_ip_2, isolated_network)
        self.validate_PublicIPAddress(
                public_ip_2, isolated_network, static_nat=True, vm=vm_4)
        firewall_rule_2 = self.create_FirewallRule(public_ip_2)
        self.verify_vsd_network(self.domain.id, isolated_network)
        self.verify_vsd_floating_ip(isolated_network, vm_4,
                                    public_ip_2.ipaddress)
        self.verify_vsd_vm(vm_4)
        self.verify_vsd_firewall_rule(firewall_rule_2)
        # self.verify_vsd_vm(vpc_vm_2)

        vm_public_ip_2 = public_ip_2.ipaddress.ipaddress
        try:
            vm_4.ssh_ip = vm_public_ip_2
            vm_4.ssh_port = self.test_data["virtual_machine"]["ssh_port"]
            vm_4.username = self.test_data["virtual_machine"]["username"]
            vm_4.password = self.test_data["virtual_machine"]["password"]
            self.debug("SSHing into VM: %s with %s" %
                       (vm_4.ssh_ip, vm_4.password))

            ssh2 = vm_4.get_ssh_client(ipaddress=vm_public_ip_2)

        except Exception as e:
            self.fail("SSH into VM failed with exception %s" % e)

        defaultipaddress2 = \
            [nic.ipaddress for nic in vpc_vm_2.nic if nic.isdefault][0]

        self.verify_pingtovmipaddress(ssh2, defaultipaddress2)

    @attr(tags=["migrateACS", "guestvmip2"],
          required_hardware="false")
    def test_13_verify_guestvmip2_when_migrating_to_nuage(self):
        """
        Verify migration of GuestVm with ip .2 still works
        when migrating isolated or vpc tier networks
        """
        isolated_network = self.create_Network(
                self.native_isolated_network_offering,
                gateway="10.13.0.1",
                netmask="255.255.255.248", account=self.account)

        self.test_data["virtual_machine"]["ipaddress"] = "10.13.0.2"
        vm_11 = self.create_VM(isolated_network)
        self.test_data["virtual_machine"]["ipaddress"] = "10.13.0.3"
        vm_12 = self.create_VM(isolated_network)
        self.test_data["virtual_machine"]["ipaddress"] = "10.13.0.4"
        vm_13 = self.create_VM(isolated_network)
        self.test_data["virtual_machine"]["ipaddress"] = "10.13.0.5"
        vm_14 = self.create_VM(isolated_network)
        self.test_data["virtual_machine"]["ipaddress"] = None
        vm_15 = self.create_VM(isolated_network)

        try:
            self.migrate_network(
                    self.nuage_isolated_network_offering_persistent,
                    isolated_network, resume=False)
        except Exception as e:
            errortext = re.search(".*errortext\s*:\s*u?'([^']+)'.*",
                                  e.message).group(1)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to implement network (with specified id) " \
                    "elements and resources as a part of network update"
        self.verifymigrationerrortext(errortext, expectstr)

        try:
            self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr,
                    isolated_network, resume=False)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to migrate network as previous migration " \
                    "left this network in transient condition. " \
                    "Specify resume as true."
        self.verifymigrationerrortext(errortext, expectstr)

        try:
            self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr,
                    isolated_network, resume=True)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to resume migrating network as network offering " \
                    "does not match previously specified network offering"
        self.verifymigrationerrortext(errortext, expectstr)

        vm_13.delete(self.api_client, expunge=True)

        try:
            self.migrate_network(
                    self.nuage_isolated_network_offering_without_vr,
                    isolated_network, resume=True)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to resume migrating network as network offering " \
                    "does not match previously specified network offering"
        self.verifymigrationerrortext(errortext, expectstr)

        try:
            self.migrate_network(
                    self.nuage_isolated_network_offering_persistent,
                    isolated_network, resume=False)
        except Exception as e:
            errortext = \
                re.search(".*errortext\s*:\s*u?(['\"])([^\\1]+)\\1.*",
                          e.message).group(2)
            self.debug("Migration fails with %s" % errortext)

        expectstr = "Failed to migrate network as previous migration " \
                    "left this network in transient condition. " \
                    "Specify resume as true."
        self.verifymigrationerrortext(errortext, expectstr)

        self.migrate_network(
                self.nuage_isolated_network_offering_persistent,
                isolated_network, resume=True)

        self.verify_vsd_network(self.domain.id, isolated_network, None)
        self.verify_vsd_vm(vm_11)
        self.verify_vsd_vm(vm_12)
        self.verify_vsd_vm(vm_14)
        self.verify_vsd_vm(vm_15)

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.api_client.updateZone(cmd)

        self.debug("Creating Native VSP VPC offering with Static NAT service "
                   "provider as VPCVR...")
        native_vpc_off = self.create_VpcOffering(
                self.test_data["vpc_offering_reduced"])
        self.validate_VpcOffering(native_vpc_off, state="Enabled")

        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter")
        vpc = self.create_Vpc(native_vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        self.debug("Creating native VPC Network Tier offering "
                   "with Static NAT service provider as VPCVR")
        native_tiernet_off = self.create_NetworkOffering(
                self.test_data["nw_offering_reduced_vpc"])
        self.validate_NetworkOffering(native_tiernet_off, state="Enabled")

        acl_list = self.create_NetworkAclList(
                name="acl", description="acl", vpc=vpc)

        self.create_NetworkAclRule(
                self.test_data["ingress_rule"], acl_list=acl_list)
        self.create_NetworkAclRule(
                self.test_data["icmprule"], acl_list=acl_list)

        self.debug("Creating a VPC tier network with Static NAT service")
        vpc_tier = self.create_Network(native_tiernet_off,
                                       gateway='10.1.1.1',
                                       vpc=vpc,
                                       acl_list=acl_list)
        self.validate_Network(vpc_tier, state="Implemented")

        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        self.debug("Deploying a VM in a vpc tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm1"
        self.test_data["virtual_machine"]["name"] = "vpcvm1"
        self.test_data["virtual_machine"]["ipaddress"] = "10.1.1.2"
        vpc_vm_1 = self.create_VM(vpc_tier)
        self.test_data["virtual_machine"]["ipaddress"] = None
        self.check_VM_state(vpc_vm_1, state="Running")

        self.debug("Deploying another VM in vpc tier")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm2"
        self.test_data["virtual_machine"]["name"] = "vpcvm2"
        vpc_vm_2 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_2, state="Running")

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.nuage_vpc_network_offering.id}]

        self.migrate_vpc(vpc, self.nuage_vpc_offering,
                         network_offering_map, resume=False)

        self.verify_vsd_network(self.domain.id, vpc_tier, vpc)
        self.verify_vsd_vm(vpc_vm_1)
        self.verify_vsd_vm(vpc_vm_2)

    @attr(tags=["migrateACS", "nativeisoonly"],
          required_hardware="false")
    def test_14_native_to_native_network_migration(self):
        """
        Verify Migration for an isolated network nativeOnly
        1. create native non-persistent isolated network
        2. migrate to native persistent isolated network, check VR state
        3. migrate back to native non-persistent network
        4. deploy VM in non-persistent isolated network
        5. acquire ip and enable staticnat
        6. migrate to native persistent isolated network
        7. migrate back to native non-persistent network
        """
        isolated_network = self.create_Network(
                self.native_isolated_network_offering, gateway="10.0.0.1",
                netmask="255.255.255.0")

        self.migrate_network(
                self.native_isolated_network_offering_persistent,
                isolated_network, resume=False)

        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")

        self.migrate_network(
                self.native_isolated_network_offering,
                isolated_network, resume=False)

        vm_1 = self.create_VM(isolated_network)
        vr = self.get_Router(isolated_network)
        self.check_Router_state(vr, "Running")
        public_ip = self.acquire_PublicIPAddress(isolated_network)
        self.create_StaticNatRule_For_VM(vm_1, public_ip, isolated_network)
        self.create_FirewallRule(public_ip)

        self.migrate_network(
                self.native_isolated_network_offering_persistent,
                isolated_network, resume=False)

        self.migrate_network(
                self.native_isolated_network_offering,
                isolated_network, resume=False)

    @attr(tags=["migrateACS", "nativevpconly"],
          required_hardware="false")
    def test_15_native_to_native_vpc_migration(self):
        """
        Verify Migration for a vpc network nativeOnly
        1. create native vpc with 2 tier networks
        2. migrate to native vpc, check VR state
        3. deploy VM in vpc tier network
        4. acquire ip and enable staticnat
        5. migrate to native vpc network
        """

        cmd = updateZone.updateZoneCmd()
        cmd.id = self.zone.id
        cmd.domain = "vpc.com"
        self.api_client.updateZone(cmd)

        self.debug("Creating Native VSP VPC offering with Static NAT service "
                   "provider as VPCVR...")
        native_vpc_off = self.create_VpcOffering(
                self.test_data["vpc_offering_reduced"])
        self.validate_VpcOffering(native_vpc_off, state="Enabled")

        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter")
        vpc = self.create_Vpc(native_vpc_off, cidr='10.1.0.0/16')
        self.validate_Vpc(vpc, state="Enabled")

        self.debug("Creating native VPC Network Tier offering "
                   "with Static NAT service provider as VPCVR")
        native_tiernet_off = self.create_NetworkOffering(
                self.test_data["nw_offering_reduced_vpc"])
        self.validate_NetworkOffering(native_tiernet_off, state="Enabled")

        acl_list = self.create_NetworkAclList(
                name="acl", description="acl", vpc=vpc)

        self.create_NetworkAclRule(
                self.test_data["ingress_rule"], acl_list=acl_list)
        self.create_NetworkAclRule(
                self.test_data["icmprule"], acl_list=acl_list)

        self.debug("Creating a VPC tier network with Static NAT service")
        vpc_tier = self.create_Network(native_tiernet_off,
                                       gateway='10.1.0.1',
                                       vpc=vpc,
                                       acl_list=acl_list)
        self.validate_Network(vpc_tier, state="Implemented")

        self.debug("Creating 2nd VPC tier network with Static NAT service")
        vpc_2ndtier = self.create_Network(native_tiernet_off,
                                          gateway='10.1.128.1',
                                          vpc=vpc,
                                          acl_list=acl_list)
        self.validate_Network(vpc_2ndtier, state="Implemented")

        vpc_vr = self.get_Router(vpc_tier)
        self.check_Router_state(vpc_vr, state="Running")

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.native_vpc_network_offering.id},
             {"networkid": vpc_2ndtier.id,
              "networkofferingid": self.native_vpc_network_offering.id}]

        self.migrate_vpc(vpc, self.native_vpc_offering,
                         network_offering_map, resume=False)

        self.debug("Deploying a VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm1"
        self.test_data["virtual_machine"]["name"] = "vpcvm1"
        vpc_vm_1 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_1, state="Running")

        self.debug("Deploying another VM in the created VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm2"
        self.test_data["virtual_machine"]["name"] = "vpcvm2"
        vpc_vm_2 = self.create_VM(vpc_tier)
        self.check_VM_state(vpc_vm_2, state="Running")

        self.debug("Deploying a VM in the 2nd VPC tier network")
        self.test_data["virtual_machine"]["displayname"] = "vpcvm12"
        self.test_data["virtual_machine"]["name"] = "vpcvm12"
        self.create_VM(vpc_2ndtier)
        self.check_VM_state(vpc_vm_2, state="Running")
        self.test_data["virtual_machine"]["displayname"] = None
        self.test_data["virtual_machine"]["name"] = None

        self.debug("Creating Static NAT rule for the deployed VM "
                   "in the created VPC network...")
        public_ip_1 = self.acquire_PublicIPAddress(vpc_tier, vpc=vpc)
        self.validate_PublicIPAddress(public_ip_1, vpc_tier)
        self.create_StaticNatRule_For_VM(vpc_vm_1, public_ip_1, vpc_tier)
        self.validate_PublicIPAddress(
                public_ip_1, vpc_tier, static_nat=True, vm=vpc_vm_1)

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.native_vpc_network_offering.id},
             {"networkid": vpc_2ndtier.id,
              "networkofferingid": self.native_vpc_network_offering.id}]
        self.migrate_vpc(vpc, self.native_vpc_offering,
                         network_offering_map, resume=False)
