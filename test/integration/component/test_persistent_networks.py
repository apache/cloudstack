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
""" Tests for Persistent Networks without running VMs feature"""
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              get_hypervisor_type, get_process_status)
from marvin.lib.base import (Account,
                             VPC,
                             VirtualMachine,
                             LoadBalancerRule,
                             Network,
                             Domain,
                             Router,
                             NetworkACL,
                             PublicIPAddress,
                             VpcOffering,
                             ServiceOffering,
                             Project,
                             NetworkOffering,
                             NATRule,
                             FireWallRule,
                             Host,
                             StaticNATRule)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               verifyNetworkState,
                               add_netscaler,
                               wait_for_cleanup,list_routers,list_hosts)
from nose.plugins.attrib import attr
from marvin.codes import PASS, FAIL, FAILED
from marvin.sshClient import SshClient
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from ddt import ddt, data
import time


@ddt
class TestPersistentNetworks(cloudstackTestCase):

    '''
    Test Persistent Networks without running VMs
    '''
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestPersistentNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.isolated_persistent_network_offering = cls.createNetworkOffering(
            "nw_off_isolated_persistent")
        cls.isolated_persistent_network_offering_netscaler =\
            cls.createNetworkOffering(
                "nw_off_isolated_persistent_netscaler"
            )
        cls.isolated_persistent_network_offering_RVR =\
            cls.createNetworkOffering(
                "nw_off_persistent_RVR"
            )
        cls.isolated_network_offering = cls.createNetworkOffering(
            "isolated_network_offering")
        cls.isolated_network_offering_netscaler = cls.createNetworkOffering(
            "nw_off_isolated_netscaler")

        cls.services["configurableData"]["netscaler"]["lbdevicededicated"] =\
            False

        # Configure Netscaler device
        # If configuration succeeds, set ns_configured to True so that
        # Netscaler tests are executed
        cls.ns_configured = False
        try:
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.services["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.ns_configured = True
        except Exception:
            cls.ns_configured = False

        # network will be deleted as part of account cleanup
        cls._cleanup = [
            cls.account,
            cls.service_offering,
            cls.isolated_persistent_network_offering,
            cls.isolated_network_offering,
            cls.isolated_persistent_network_offering_RVR,
            cls.isolated_persistent_network_offering_netscaler,
            cls.isolated_network_offering_netscaler]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def createNetworkOffering(cls, network_offering_type):
        network_offering = NetworkOffering.create(
            cls.api_client,
            cls.services[network_offering_type],
            conservemode=False
        )
        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            network_offering,
            cls.api_client,
            id=network_offering.id,
            state="enabled")
        return network_offering

    def checkRouterAccessibility(self, router):
        """Check if router is accessible through its linklocalip"""

        hypervisor = str(get_hypervisor_type(self.api_client))

        if hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH is done via management server for Vmware and Hyper-V
            sourceip = self.api_client.connection.mgtSvr
        else:
            # For others, we will have to get the ipaddress of host connected
            # to vm
            hosts = Host.list(self.api_client, id=router.hostid)
            self.assertEqual(
                validateList(hosts)[0],
                PASS,
                "hosts list validation failed, list is %s" %
                hosts)
            host = hosts[0]
            sourceip = host.ipaddress
            # end else

        try:
            sshClient = SshClient(
                host=sourceip,
                port=self.services['configurableData']['host']["publicport"],
                user=self.hostConfig['username'],
                passwd=self.hostConfig["password"])
            res = sshClient.execute("ping -c 1 %s" % (
                router.linklocalip
            ))
            self.debug("SSH result: %s" % res)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (sourceip, e)
                      )
        result = str(res)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to router should be successful"
        )
        return

    def verifyVmExpungement(self, virtual_machine):
        """verify if vm is expunged"""
        isVmExpunged = False
        # Verify if it is expunged
        retriesCount = 20
        while True:
            vms = VirtualMachine.list(self.api_client, id=virtual_machine.id)
            # When vm is expunged, list will be None
            if vms is None:
                isVmExpunged = True
                break
            elif retriesCount == 0:
                break
            time.sleep(60)
            retriesCount -= 1
            # end while

        if not isVmExpunged:
            self.fail("Failed to expunge vm even after 20 minutes")
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_network_state_after_destroying_vms(self):
        # steps
        # 1. Create an isolated persistent network
        # 2. Deploy virtual machine in network
        # 3. destroy created virtual machine
        #
        # validation
        # 1. Persistent network state should be implemented before VM creation
        #    and have some vlan assigned
        # 2. virtual machine should be created successfully
        # 3. Network state should be implemented even after destroying all vms
        # in network

        # Creating isolated persistent network
        network = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.cleanup.append(network)
        response = verifyNetworkState(
            self.apiclient,
            network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            network.vlan,
            "vlan must not be null for persistent network")

        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network.id],
                serviceofferingid=self.service_offering.id,
                accountid=self.account.name,
                domainid=self.domain.id)
            virtual_machine.delete(self.apiclient)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Verify VM is expunged
        self.verifyVmExpungement(virtual_machine)

        # wait for time such that, network is cleaned up
        # assuming that it will change its state to allocated after this much
        # period
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        verifyNetworkState(self.api_client, network.id, "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_shared_network_offering_with_persistent(self):
        # steps
        # 1. create shared network offering with persistent field enabled
        #
        # validation
        # 1. network offering should throw an exception
        try:
            shared_persistent_network_offering = self.createNetworkOffering(
                "nw_offering_shared_persistent")
            shared_persistent_network_offering.delete(self.apiclient)
            self.fail(
                "Shared network got created with ispersistent flag\
                True in the offering, it should have failed")
        except Exception:
            pass

    @attr(tags=["advanced"])
    def test_persistent_network_offering_with_VPCVR_services(self):
        # steps
        # 1. create network offering with persistent field enabled and
        #    all the services through VpcVirtualRouter
        #
        # validation
        # 1. network offering should be created successfully
        try:
            persistent_network_offering_VPCVR = self.createNetworkOffering(
                "nw_off_persistent_VPCVR_LB")
            persistent_network_offering_VPCVR.delete(self.apiclient)
        except Exception as e:
            self.fail(
                "Failed creating persistent network offering\
                with VPCVR services: %s" %
                e)

    @attr(tags=["advanced"])
    def test_list_persistent_network_offering(self):
        # steps
        # 1. create isolated network offering with ispersistent True
        # 2. List network offerings by id and check ispersistent flag
        #
        # validation
        # 1. ispersistent flag should list as True
        network_offering = self.createNetworkOffering(
            "nw_off_isolated_persistent")
        self.cleanup.append(network_offering)
        nw_offering_list = NetworkOffering.list(
            self.api_client,
            id=network_offering.id)
        self.assertEqual(
            validateList(nw_offering_list)[0],
            PASS,
            "network offerings' list validation failed, list is %s" %
            nw_offering_list)
        self.assertEqual(
            nw_offering_list[0].ispersistent,
            True,
            "ispersistent flag should be true for the network offering")
        return

    @data("LB-VR", "LB-NS")
    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_upgrade_to_persistent_services_VR(self, value):

        # This test is run against two networks (one with LB as virtual router
        # and other one is LB with Netscaler)
        # All other services through VR

        # steps
        # 1. create isolated network with network offering which
        #    has ispersistent field disabled
        # 2. upgrade isolated network offering to network offering
        #    which has ispersistent field enabled
        # 3. Deploy VM ,acquire IP, create Firewall, NAT rules
        # 4. Verify the working of NAT, Firewall rules
        # 5. Delete VM
        # 6. Verify network state after network cleanup interval
        #
        # validation
        # 1. update of network should happen successfully
        # 2. NAT and Firewall rule should work as expected
        # 3. After network clenaup interval, network state should be
        # implemented and have some vlan assigned

        # Set network offering as required (either LB through VR or LB through
        # Netscaler)
        networkOffering = self.isolated_network_offering

        # This will be true in case of Netscaler case, you have to change cidr
        # while updating network
        changecidr = False

        # In case Netscaler is used for LB
        if value == "LB-NS":
            if self.ns_configured:
                networkOffering = self.isolated_network_offering_netscaler
                changecidr = True
            else:
                self.skipTest(
                    "Skipping - this test required netscaler\
                    configured in the network")

        # Create Account
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        # create network with the appropriate offering (not persistent)
        isolated_network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=networkOffering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.api_client,
            isolated_network.id,
            "allocated")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        # Update the network with persistent network offering
        isolated_network.update(
            self.apiclient,
            networkofferingid=self.isolated_persistent_network_offering.id,
            changecidr=changecidr,
            forced=True)

        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Acquire public ip and open firewall for it
        self.debug(
            "Associating public IP for network: %s" %
            isolated_network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])

        # Create NAT rule
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=isolated_network.id)

        # Check if SSH works
        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        # Delete VM
        virtual_machine.delete(self.api_client)

        # Verify VM is expunged
        self.verifyVmExpungement(virtual_machine)

        # wait for time such that, network is cleaned up
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        # Check network state now, this will bolster that network updation has
        # taken effect
        response = verifyNetworkState(
            self.api_client,
            isolated_network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_upgrade_network_VR_to_PersistentRVR(self):
        # steps
        # 1. create isolated network with network offering which has
        #    ispersistent field disabled (services through VR)
        # 2. upgrade isolated network offering to network offering which
        #    has ispersistent field enabled (with RVR)
        # 3. Deploy VM ,acquire IP, create Firewall, NAT rules
        # 4. Verify the working of NAT, Firewall rules
        # 5. Delete VM
        # 6. Verify network state after network cleanup interval
        #
        # validation
        # 1. update of network should happen successfully
        # 2. NAT and Firewall rule should work as expected
        # 3. After network clenaup interval, network state should be
        # implemented and have some vlan assigned

        # Create Account and isolated network in it
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)
        isolated_network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.api_client,
            isolated_network.id,
            "allocated")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        # Update network with network offering which has RVR
        isolated_network.update(
            self.apiclient,
            networkofferingid=self.isolated_persistent_network_offering_RVR.id)

        # Deploy VM
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Acquire public IP and open firewall rule, create NAT rule
        self.debug(
            "Associating public IP for network: %s" %
            isolated_network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])

        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=isolated_network.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        virtual_machine.delete(self.api_client)

        # Verify VM is expunged
        self.verifyVmExpungement(virtual_machine)

        # wait for time such that, network is cleaned up
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        # Check network state now, this will bolster that network updation has
        # taken effect
        response = verifyNetworkState(
            self.api_client,
            isolated_network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return

    @attr(tags=["advanced", "advancedns"])
    def test_upgrade_network_NS_to_persistent_NS(self):
        # steps
        # 1. create isolated network with network offering which
        #    has ispersistent field disabled
        #    and LB service through Netscaler
        # 2. upgrade isolated network offering to network offering
        #    which has ispersistent field enabled
        #    and LB service through Netscaler
        # 3. Deploy VM ,acquire IP, create Firewall, NAT rules
        # 4. Verify the working of NAT, Firewall rules
        # 5. Delete VM
        # 6. Verify network state after network cleanup interval
        #
        # validation
        # 1. update of network should happen successfully
        # 2. NAT and Firewall rule should work as expected
        # 3. After network clenaup interval, network state should be
        # implemented and have some vlan assigned

        # Skip test if Netscaler is not configured
        if not self.ns_configured:
            self.skipTest(
                "Skipping - this test required netscaler\
                configured in the network")

        # Create Account and create isolated network in it
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)
        isolated_network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_network_offering_netscaler.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.api_client,
            isolated_network.id,
            "allocated")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        isolated_network.update(
            self.apiclient,
            networkofferingid=(
                self.isolated_persistent_network_offering_netscaler.id),
            changecidr=True)

        # Deploy VM
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        self.debug(
            "Associating public IP for network: %s" %
            isolated_network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])

        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=isolated_network.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        virtual_machine.delete(self.api_client)

        # Verify VM is expunged
        self.verifyVmExpungement(virtual_machine)

        # wait for time such that, network is cleaned up
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        # Check network state now, this will bolster that network updation has
        # taken effect
        response = verifyNetworkState(
            self.api_client,
            isolated_network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return

    @data("LB-VR", "LB-Netscaler")
    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_pf_nat_rule_persistent_network(self, value):

        # This test shall run with two scenarios, one with LB services
        # through VR and other through LB service
        # through Netscaler"""

        # steps
        # 1. create isolated network with network offering which has
        #    ispersistent field enabled
        #    and LB service through VR or Netscaler
        # 2. Check routers belonging to network and verify that router
        #    is reachable through host using linklocalip
        # 3. Deploy VM ,acquire IP, create Firewall, NAT rules
        # 4. Verify the working of NAT, Firewall rules
        #
        # validation
        # 1. Router should be reachable
        # 2. NAT and Firewall rule should work as expected

        # Set network offering according to data passed to test case
        networkOffering = self.isolated_persistent_network_offering
        if value == "LB-Netscaler":
            if self.ns_configured:
                networkOffering = (
                    self.isolated_persistent_network_offering_netscaler)
            else:
                self.skipTest(
                    "Skipping - this test required netscaler\
                    configured in the network")

        # Create account and network in it
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)
        isolated_persistent_network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=networkOffering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.assertEqual(
            str(
                isolated_persistent_network.state).lower(),
            "implemented",
            "network state should be implemented, it is %s" %
            isolated_persistent_network.state)
        self.assertIsNotNone(
            isolated_persistent_network.vlan,
            "vlan must not be null for persistent network")

        # Check if router is assigned to the persistent network
        routers = Router.list(self.api_client, account=account.name,
                              domainid=account.domainid,
                              networkid=isolated_persistent_network.id)

        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed, list is %s" %
            routers)
        router = routers[0]

        # Check if router if reachable from the host
        self.checkRouterAccessibility(router)

        # Deploy VM in the network
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_persistent_network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Acquire IP address, create Firewall, NAT rule
        self.debug(
            "Associating public IP for network: %s" %
            isolated_persistent_network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_persistent_network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=isolated_persistent_network.id)

        # Check working of PF, NAT rules
        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_persistent_network_with_RVR(self):
        # steps
        # 1. create account and isolated network with network
        #    offering which has ispersistent field enabled
        #    and supporting Redundant Virtual Router in it
        # 2. Check the Master and Backup Routers are present
        # 3. Deploy VM ,acquire IP, create Firewall, NAT rules
        # 4. Verify the working of NAT, Firewall rules
        #
        # validation
        # 1. Two routers should belong to the network and
        #    they should be reachable from host
        # 2. NAT and Firewall rule should work as expected

        # Create account and create isolated persistent network with RVR in it
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        # Create isolated persistent network with RVR
        isolated_persistent_network_RVR = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering_RVR.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.assertEqual(
            str(
                isolated_persistent_network_RVR.state).lower(),
            "implemented",
            "network state should be implemented, it is %s" %
            isolated_persistent_network_RVR.state)
        self.assertIsNotNone(
            isolated_persistent_network_RVR.vlan,
            "vlan must not be null for persistent network")

        # Check if two routers belong to the network
        self.debug(
            "Listing routers for network: %s" %
            isolated_persistent_network_RVR.name)
        routers = Router.list(self.api_client, listall=True,
                              networkid=isolated_persistent_network_RVR.id)

        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed, list is %s" %
            routers)
        self.assertEqual(
            len(routers),
            2,
            "Length of the list router should be 2 (Backup & master)")

        # Check if routers are reachable from the host
        for router in routers:
            self.checkRouterAccessibility(router)

        # Deploy VM
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_persistent_network_RVR.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Acquire public ip, create Firewall, NAT rule
        self.debug(
            "Associating public IP for network: %s" %
            isolated_persistent_network_RVR.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_persistent_network_RVR.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=isolated_persistent_network_RVR.id)

        # Check if Firewall, NAT rule work as expected
        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_vm_deployment_two_persistent_networks(self):
        # steps
        # 1. Deploy VM in two persistent networks
        # 2. Check working of NAT, Firewall rules in both the networks
        #
        # validation
        # 1. VM should be deployed successfully in two networks
        # 2. All network rules should work as expected

        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)
        isolated_persistent_network_1 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.assertEqual(
            str(
                isolated_persistent_network_1.state).lower(),
            "implemented",
            "network state should be implemented, it is %s" %
            isolated_persistent_network_1.state)
        self.assertIsNotNone(
            isolated_persistent_network_1.vlan,
            "vlan must not be null for persistent network")

        isolated_persistent_network_2 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.assertEqual(
            str(
                isolated_persistent_network_2.state).lower(),
            "implemented",
            "network state should be implemented, it is %s" %
            isolated_persistent_network_2.state)
        self.assertIsNotNone(
            isolated_persistent_network_2.vlan,
            "vlan must not be null for persistent network")

        self.debug(
            "Listing routers for network: %s" %
            isolated_persistent_network_1.name)
        routers_nw_1 = Router.list(self.api_client, listall=True,
                                   networkid=isolated_persistent_network_1.id)

        self.assertEqual(
            validateList(routers_nw_1)[0],
            PASS,
            "Routers list validation failed, list is %s" %
            routers_nw_1)

        # Check if router is reachable from the host
        for router in routers_nw_1:
            self.checkRouterAccessibility(router)

        self.debug(
            "Listing routers for network: %s" %
            isolated_persistent_network_2.name)
        routers_nw_2 = Router.list(self.api_client, listall=True,
                                   networkid=isolated_persistent_network_2.id)

        self.assertEqual(
            validateList(routers_nw_2)[0],
            PASS,
            "Routers list validation failed, list is %s" %
            routers_nw_2)

        # Check if router is reachable from the host
        for router in routers_nw_2:
            self.checkRouterAccessibility(router)
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_persistent_network_1.id,
                    isolated_persistent_network_2.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        self.debug(
            "Associating public IP for network: %s" %
            isolated_persistent_network_1.id)
        ipaddress_nw_1 = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_persistent_network_1.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress_nw_1.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])

        self.debug(
            "Associating public IP for network: %s" %
            isolated_persistent_network_2.id)
        ipaddress_nw_2 = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_persistent_network_2.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress_nw_2.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress_nw_1.ipaddress.id,
            networkid=isolated_persistent_network_1.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress_nw_1.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress_nw_1.ipaddress.ipaddress))

        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress_nw_2.ipaddress.id,
            networkid=isolated_persistent_network_2.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress_nw_2.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress_nw_2.ipaddress.ipaddress))
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_vm_deployment_persistent_and_non_persistent_networks(self):
        # steps
        # 1. create account and create two networks in it
        #    (persistent and non persistent)
        # 2. Deploy virtual machine in these two networks
        # 3. Associate ip address with both the accounts and
        #    create firewall,port forwarding rule
        # 4. Try to SSH to VM through both the IPs
        #
        # validation
        # 1. Both persistent and non persistent networks should be created
        # 2. virtual machine should be created successfully
        # 3. SSH should be successful through both the IPs

        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)
        network_1 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.assertEqual(
            str(
                network_1.state).lower(),
            "implemented",
            "network state should be implemented, it is %s" %
            network_1.state)
        self.assertIsNotNone(
            network_1.vlan,
            "vlan must not be null for persistent network")

        network_2 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)

        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network_1.id,
                    network_2.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network_1.id)
        ipaddress_nw_1 = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network_1.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress_nw_1.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress_nw_1.ipaddress.id,
            networkid=network_1.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress_nw_1.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress_nw_1.ipaddress.ipaddress))

        self.debug("Associating public IP for network: %s" % network_2.id)
        ipaddress_nw_2 = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network_2.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress_nw_2.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress_nw_2.ipaddress.id,
            networkid=network_2.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress_nw_2.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress_nw_2.ipaddress.ipaddress))
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_change_persistent_network_to_non_persistent(self):
        # steps
        # 1. Create a persistent network and deploy VM in it
        # 2. Update network with non persistent network offering
        # 3. Acquire IP, create NAT, firewall rules
        # 4. Test NAT, Firewall rules
        # 5. Delete VM
        # 6. Check the network state after network clenaup interval
        #
        # validation
        # 1. Network updation should be successful
        # 2. Network rules should work as expected
        # 3. Network state should be allocated after network cleanup interval

        # Create account and create persistent network in it
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)
        network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        self.assertEqual(str(network.state).lower(),
                         "implemented",
                         "network state should be implemented, it is %s"
                         % network.state)
        self.assertIsNotNone(
            network.vlan,
            "vlan must not be null for persistent network")

        # Update network with non persistent network offering
        network.update(
            self.apiclient,
            networkofferingid=self.isolated_network_offering.id)

        # Deploy VM, acquire IP, create NAT, firewall rules
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        self.debug("Associating public IP for network: %s" % network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=network.id)

        # Verify working of network rules
        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        # Delete VM
        virtual_machine.delete(self.api_client)

        # Verify VM is expunged
        self.verifyVmExpungement(virtual_machine)

        # wait for time such that, network is cleaned up
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        # Check network state now, this will bolster that network updation has
        # taken effect
        response = verifyNetworkState(self.api_client, network.id, "allocated")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_delete_account(self):
        # steps
        # 1. create persistent network and deploy VM in it
        # 2. Deploy VM in it and acquire IP address, create NAT, firewall rules
        # 3. Delete the account in which network is created
        #
        # validation
        # 1. Persistent network state should be implemented before VM creation
        #    and have some vlan assigned
        # 2. Network rules should work as expected
        # 3. Network and IPs should be freed after account is deleted

        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)

        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)
        self.debug("Associating public IP for network: %s" % network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])
        NATRule.create(
            self.api_client,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=ipaddress.ipaddress.id,
            networkid=network.id)

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        # Delete the account
        account.delete(self.api_client)

        # Verify the resources belonging to account have been cleaned up
        networks = Network.list(self.apiclient, id=network.id)
        self.assertEqual(
            validateList(networks)[0],
            FAIL,
            "network list should be enmpty, it is %s" %
            networks)

        public_ips = PublicIPAddress.list(
            self.apiclient,
            id=ipaddress.ipaddress.id)
        self.assertEqual(
            validateList(public_ips)[0],
            FAIL,
            "Public Ip list be empty, it is %s" %
            public_ips)

        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_volume_delete_event_errorState(self):
        """
        @summary: Test volume delete event generation in error state condition
        @Steps:
        Step1: Create  a network using network created in Step1
        Step2: Verifying that  network creation is successful
        Step3: Login to Virtual router and add iptable  rule to block insertion of vm rules
        Step6: deploy a vm using network created in step2
        Step7: check the Vm status for failure
        Step8: destroy and expunge the vm
        Step9: list the generated events for volume delete event.
        """

        # Listing all the networks available

        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id)
        network = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.apiclient,
            network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            network.vlan,
            "vlan must not be null for persistent network")
        try:
            list_router_response = list_routers(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid
            )

            self.assertEqual(validateList(list_router_response)[0], PASS, "Check list response returns a valid list")
            router = list_router_response[0]

            self.debug("Router ID: %s, state: %s" % (router.id, router.state))

            self.assertEqual(
                router.state,
                'Running',
                "Check list router response for router state"
            )
            self.hypervisor = self.testClient.getHypervisorInfo()
            if self.hypervisor.lower() in ('vmware', 'hyperv'):
                result = get_process_status(
                    self.apiclient.connection.mgtSvr,
                    22,
                    self.apiclient.connection.user,
                    self.apiclient.connection.passwd,
                    router.linklocalip,
                    "iptables -I INPUT 1 -j DROP",
                    hypervisor=self.hypervisor
                )
            else:
                try:
                    hosts = list_hosts(
                        self.apiclient,
                        zoneid=router.zoneid,
                        type='Routing',
                        state='Up',
                        id=router.hostid
                    )
                    self.assertEqual(validateList(hosts)[0],PASS,"Check list host returns a valid list")
                    host = hosts[0]
                    result = get_process_status(
                        host.ipaddress,22,
                        self.hostConfig["username"],
                        self.hostConfig["password"],
                        router.linklocalip,
                    "iptables -I INPUT 1 -j DROP"
                    )
                except Exception as e:
                    raise Exception("Exception raised in accessing/running the command on hosts  : %s " % e)
        except Exception as e:
            raise Exception("Exception raised in getting hostcredentials: %s " % e)

        with self.assertRaises(Exception) as context:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network.id],
                serviceofferingid=self.service_offering.id,
                accountid=self.account.name,
                domainid=self.domain.id)
        #self.assertTrue('This is broken' in context.exception)
        try:
            account.delete(self.api_client)
        except Exception as e:
            self.cleanup.append(account)
        qresultset = self.dbclient.execute(
             "select id from usage_event where type = '%s' ORDER BY id DESC LIMIT 1;" %
             str("VOLUME.DELETE"))
        self.assertNotEqual(
             len(qresultset),
             0,
             "Check DB Query result set")
        return

@ddt
class TestAssignVirtualMachine(cloudstackTestCase):

    """Test Persistent Network creation with
    assigning VM to different account/domain
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestAssignVirtualMachine,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return template\
                with description %s" % cls.services["ostype"]
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.isolated_persistent_network_offering = cls.createNetworkOffering(
            "nw_off_isolated_persistent")
        cls.isolated_persistent_network_offering_RVR =\
            cls.createNetworkOffering(
                "nw_off_persistent_RVR"
            )
        cls.persistent_network_offering_netscaler = cls.createNetworkOffering(
            "nw_off_isolated_persistent_netscaler")

        cls.services["configurableData"]["netscaler"]["lbdevicededicated"] =\
            False

        # Configure Netscaler device
        # If configuration succeeds, set ns_configured to True so that
        # Netscaler tests are executed
        cls.ns_configured = False
        try:
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.services["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.ns_configured = True
        except Exception:
            cls.ns_configured = False

        # network will be deleted as part of account cleanup
        cls._cleanup = [
            cls.service_offering, cls.isolated_persistent_network_offering,
            cls.isolated_persistent_network_offering_RVR,
            cls.persistent_network_offering_netscaler
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def createNetworkOffering(cls, network_offering_type):
        network_offering = NetworkOffering.create(
            cls.api_client,
            cls.services[network_offering_type],
            conservemode=False
        )
        # Update network offering state from disabled to enabled.
        NetworkOffering.update(
            network_offering,
            cls.api_client,
            id=network_offering.id,
            state="enabled")
        return network_offering

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data("VR", "RVR", "LB-NS")
    @attr(tags=["advanced", "advancedns"])
    def test_assign_vm_different_account_VR(self, value):

        # This test shall be run with three types of persistent networks
        # a) All services through VR
        # b) LB service through Netscaler
        # c) with Redundant Virtual Router facility

        # steps
        # 1. create two accounts (account1 and account2)
        # 2. Create a persistent network (with VR/RVR/Netscaler-LB
        #    with VR services) in account1 and deploy VM in it with
        #    this network
        # 3. Stop the VM and assign the VM to account2
        #
        # validation
        # 1. Assign VM operation should be successful
        # 2. New network should be created in the other account

        # Create Accounts
        account_1 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account_1)

        account_2 = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account_2)

        # Set the network offering according to the test scenario (data passed
        # to the test case
        if value == "VR":
            network_offering = self.isolated_persistent_network_offering
        elif value == "RVR":
            network_offering = self.isolated_persistent_network_offering_RVR
        elif value == "LB-NS":
            if self.ns_configured:
                network_offering = self.persistent_network_offering_netscaler
            else:
                self.skipTest(
                    "This test requires netscaler to be\
                    configured in the network")

        network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=network_offering.id,
            accountid=account_1.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.api_client,
            network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            network.vlan,
            "vlan must not be null for persistent network")

        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account_1.name,
                domainid=self.domain.id)

            virtual_machine.stop(self.apiclient)

            # Assign virtual machine to different account
            virtual_machine.assign_virtual_machine(
                self.apiclient,
                account=account_2.name,
                domainid=self.domain.id)

            # Start VM
            virtual_machine.start(self.apiclient)

            # Verify that new network is created in other account
            networks = Network.list(
                self.apiclient,
                account=account_2.name,
                domainid=account_2.domainid)
            self.assertEqual(
                validateList(networks)[0],
                PASS,
                "networks list validation failed, list is %s" %
                networks)
        except Exception as e:
            self.fail("Exception occured: %s" % e)
        return


@ddt
class TestProjectAccountOperations(cloudstackTestCase):

    """Test suspend/disable/lock account/project operations
    when they have persistent network
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestProjectAccountOperations,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return\
                template with description %s" % cls.services["ostype"]
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.isolated_persistent_network_offering = NetworkOffering.create(
            cls.api_client,
            cls.services["nw_off_isolated_persistent"],
            conservemode=False
        )
        # Update network offering state from disabled to enabled.
        cls.isolated_persistent_network_offering.update(
            cls.api_client,
            state="enabled")

        # network will be deleted as part of account cleanup
        cls._cleanup = [
            cls.service_offering, cls.isolated_persistent_network_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
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
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @data("locked", "disabled")
    @attr(tags=["advanced"])
    def test_account_operations(self, value):
        # steps
        # 1. create account and create persistent network in it
        # 2. Disable/lock the account
        #
        # validation
        # 3. Wait for network cleanup interval and verify that network
        #    is not cleaned up and it is still in implemented state

        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.api_client,
            network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            network.vlan,
            "vlan must not be null for persistent network")

        if value == "disabled":
            account.disable(self.apiclient)
        elif value == "locked":
            account.disable(self.apiclient, lock=True)

        accounts = Account.list(self.apiclient, id=account.id)
        self.assertEqual(
            validateList(accounts)[0],
            PASS,
            "accounts list validation failed, list id %s" %
            accounts)
        self.assertEqual(str(accounts[0].state).lower(
        ), value, "account state should be %s, it is %s"
            % (value, accounts[0].state))

        # Wait for network cleanup interval
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        networks = Network.list(
            self.apiclient,
            account=account.name,
            domainid=account.domainid)
        self.assertEqual(
            validateList(networks)[0],
            PASS,
            "networks list validation failed, list is %s" %
            networks)

        response = verifyNetworkState(
            self.api_client,
            networks[0].id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            networks[0].vlan,
            "vlan must not be null for persistent network")
        return

    @attr(tags=["advanced"])
    def test_project_operations(self):
        # steps
        # 1. create account and create persistent network in it
        # 2. Add account to project
        # 3. Suspend the project
        #
        # validation
        # 1. Verify that account has been added to the project
        # 2. Wait for network cleanup interval and verify that network
        #    is not cleaned up and it is still in
        #    implemented state

        # Create Account
        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        # Create Project
        project = Project.create(self.apiclient, self.services["project"])
        self.cleanup.append(project)

        network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)

        # Add account to the project
        project.addAccount(self.apiclient, account=account.name)

        # Verify the account name in the list of accounts belonging to the
        # project
        projectAccounts = Project.listAccounts(
            self.apiclient,
            projectid=project.id)
        self.assertEqual(
            validateList(projectAccounts)[0],
            PASS,
            "project accounts list validation failed, list is %s" %
            projectAccounts)

        accountNames = [
            projectAccount.account for projectAccount in projectAccounts]
        self.assertTrue(
            account.name in accountNames,
            "account %s is not present in account list %s of project %s" %
            (account.name,
             accountNames,
             project.id))

        # Suspend Project
        project.suspend(self.apiclient)

        # Verify the project is suspended
        projects = Project.list(self.apiclient, id=project.id)
        self.assertEqual(
            validateList(projects)[0],
            PASS,
            "projects list validation failed, list is %s" %
            projects)
        self.assertEqual(
            str(
                projects[0].state).lower(),
            "suspended",
            "project state should be suspended, it is %s" %
            projects[0].state)

        # Wait for network cleanup interval
        wait_for_cleanup(
            self.api_client, [
                "network.gc.interval", "network.gc.wait"])

        response = verifyNetworkState(
            self.apiclient,
            network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        return


@ddt
class TestRestartPersistentNetwork(cloudstackTestCase):

    """Test restart persistent network with cleanup parameter true and false
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestRestartPersistentNetwork,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return\
                template with description %s" % cls.services["ostype"]
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.isolated_persistent_network_offering = NetworkOffering.create(
            cls.api_client,
            cls.services["nw_off_isolated_persistent_lb"],
            conservemode=False)

        cls.isolated_persistent_network_offering_netscaler =\
            NetworkOffering.create(
                cls.api_client,
                cls.services["nw_off_isolated_persistent_netscaler"],
                conservemode=False
            )
        # Update network offering state from disabled to enabled.
        cls.isolated_persistent_network_offering.update(
            cls.api_client,
            state="enabled")
        cls.isolated_persistent_network_offering_netscaler.update(
            cls.api_client,
            state="enabled")

        cls.services["configurableData"]["netscaler"]["lbdevicededicated"] =\
            False

        # Configure Netscaler device
        # If configuration succeeds, set ns_configured to True so that
        # Netscaler tests are executed
        cls.ns_configured = False
        try:
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.services["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
            cls.ns_configured = True
        except Exception:
            cls.ns_configured = False

        # network will be deleted as part of account cleanup
        cls._cleanup = [
            cls.service_offering, cls.isolated_persistent_network_offering,
            cls.isolated_persistent_network_offering_netscaler
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
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
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def checkRouterAccessibility(self, router):
        """Check if router is accessible through its linklocalip"""

        hypervisor = str(get_hypervisor_type(self.api_client))

        if hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH is done via management server for Vmware and Hyper-V
            sourceip = self.api_client.connection.mgtSvr
        else:
            # For others, we will have to get the ipaddress of host connected
            # to vm
            hosts = Host.list(self.api_client, id=router.hostid)
            self.assertEqual(
                validateList(hosts)[0],
                PASS,
                "hosts list validation failed, list is %s" %
                hosts)
            host = hosts[0]
            sourceip = host.ipaddress
            # end else

        try:
            sshClient = SshClient(
                host=sourceip,
                port=self.services['configurableData']['host']["publicport"],
                user=self.hostConfig['username'],
                passwd=self.hostConfig['password'])
            res = sshClient.execute("ping -c 1 %s" % (
                router.linklocalip
            ))
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (sourceip, e)
                      )
        result = str(res)
        self.assertEqual(
            result.count("1 received"),
            1,
            "ping to router should be successful"
        )
        return

    @data("true", "false")
    @attr(tags=["advanced"], required_hardware="true")
    def test_cleanup_persistent_network(self, value):
        # steps
        # 1. Create account and create persistent network in it
        # 2. Verify that router is reachable from the host
        # 3. Acquire public IP, open firewall and create LB rule
        # 4. Restart the network with clenup parameter true/false
        # 5. Check network state after restart, it should be implemented
        # 6. Deploy VM, assign LB rule to it, and verify the LB rule

        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        isolated_persistent_network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.apiclient,
            isolated_persistent_network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            isolated_persistent_network.vlan,
            "vlan must not be null for persistent network")

        self.debug(
            "Listing routers for network: %s" %
            isolated_persistent_network.name)
        routers = Router.list(self.api_client, listall=True,
                              networkid=isolated_persistent_network.id)

        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed, list is %s" %
            routers)

        # Check if router is reachable from the host
        for router in routers:
            self.checkRouterAccessibility(router)

        self.debug(
            "Associating public IP for network: %s" %
            isolated_persistent_network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_persistent_network.id)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=ipaddress.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"])

        # Create LB Rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=ipaddress.ipaddress.id,
            accountid=account.name,
            networkid=isolated_persistent_network.id,
            domainid=account.domainid)

        # Restart Network
        isolated_persistent_network.restart(self.apiclient, cleanup=value)

        # List networks
        networks = Network.list(
            self.apiclient,
            account=account.name,
            domainid=account.domainid)
        self.assertEqual(
            validateList(networks)[0],
            PASS,
            "networks list validation failed, list is %s" %
            networks)

        verifyNetworkState(self.apiclient, networks[0].id, "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            networks[0].vlan,
            "vlan must not be null for persistent network")

        # Deploy VM
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_persistent_network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        lb_rule.assign(self.api_client, [virtual_machine])

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress,
                port=self.services["lbrule"]["publicport"])
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        return

    @data("true", "false")
    @attr(tags=["advanced", "advancedns"])
    def test_cleanup_persistent_network_lb_netscaler(self, value):
        # steps
        # 1. Create account and create persistent network in
        #    it with LB service provided by netscaler
        # 2. Verify that router is reachable from the host
        # 3. Acquire public IP, open firewall and create LB rule
        # 4. Restart the network with clenup parameter true/false
        # 5. Check network state after restart, it should be implemented
        # 6. Deploy VM, assign LB rule to it, and verify the LB rule

        if not self.ns_configured:
            self.skipTest(
                "This test required netscaler to be configured in the network")

        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        isolated_persistent_network = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=(
                self.isolated_persistent_network_offering_netscaler.id),
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id)
        response = verifyNetworkState(
            self.apiclient,
            isolated_persistent_network.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            isolated_persistent_network.vlan,
            "vlan must not be null for persistent network")

        self.debug(
            "Listing routers for network: %s" %
            isolated_persistent_network.name)
        routers = Router.list(self.api_client, listall=True,
                              networkid=isolated_persistent_network.id)

        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "Routers list validation failed, list is %s" %
            routers)

        # Check if router is reachable from the host
        for router in routers:
            self.checkRouterAccessibility(router)

        self.debug(
            "Associating public IP for network: %s" %
            isolated_persistent_network.id)
        ipaddress = PublicIPAddress.create(
            self.api_client,
            accountid=account.name,
            zoneid=self.zone.id,
            domainid=account.domainid,
            networkid=isolated_persistent_network.id)

        # Create LB Rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=ipaddress.ipaddress.id,
            accountid=account.name,
            networkid=isolated_persistent_network.id,
            domainid=account.domainid)

        # Restart Network
        isolated_persistent_network.restart(self.apiclient, cleanup=value)

        # List networks
        networks = Network.list(
            self.apiclient,
            account=account.name,
            domainid=account.domainid)
        self.assertEqual(
            validateList(networks)[0],
            PASS,
            "networks list validation failed, list is %s" %
            networks)

        response = verifyNetworkState(
            self.apiclient,
            networks[0].id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            networks[0].vlan,
            "vlan must not be null for persistent network")

        # Deploy VM
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    isolated_persistent_network.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        lb_rule.assign(self.api_client, [virtual_machine])

        try:
            virtual_machine.get_ssh_client(
                ipaddress=ipaddress.ipaddress.ipaddress)
        except Exception as e:
            self.fail(
                "Exception while SSHing to VM %s with IP %s" %
                (virtual_machine.id, ipaddress.ipaddress.ipaddress))

        return


@ddt
class TestVPCNetworkOperations(cloudstackTestCase):

    """Test VPC network operations consisting persistent networks
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestVPCNetworkOperations,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        if cls.template == FAILED:
            assert False, "get_template() failed to return\
                template with description %s" % cls.services[
                "ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offerings"]["small"]
        )
        cls.persistent_network_offering_NoLB = NetworkOffering.create(
            cls.api_client,
            cls.services["nw_off_persistent_VPCVR_NoLB"],
            conservemode=False)
        # Update network offering state from disabled to enabled.
        cls.persistent_network_offering_NoLB.update(
            cls.api_client,
            state="enabled")

        cls.persistent_network_offering_LB = NetworkOffering.create(
            cls.api_client,
            cls.services["nw_off_persistent_VPCVR_LB"],
            conservemode=False)
        # Update network offering state from disabled to enabled.
        cls.persistent_network_offering_LB.update(
            cls.api_client,
            state="enabled")

        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"])
        cls.vpc_off.update(cls.api_client, state='Enabled')

        # network will be deleted as part of account cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.persistent_network_offering_NoLB,
            cls.vpc_off,
            cls.persistent_network_offering_LB]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
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
            # Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup[:] = []
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def GetAssociatedIpForNetwork(self, networkid, vpcid, account):
        """ Associate IP address with the network and open firewall for it
            return associated IPaddress"""

        ipaddress = PublicIPAddress.create(
            self.api_client,
            zoneid=self.zone.id,
            networkid=networkid,
            vpcid=vpcid,
            accountid=account.name,
            domainid=account.domainid)

        return ipaddress

    def CreateIngressEgressNetworkACLForNetwork(self, networkid):

        try:
            ingressAcl = NetworkACL.create(
                self.apiclient,
                networkid=networkid,
                services=self.services["natrule"],
                traffictype='Ingress')
            egressAcl = NetworkACL.create(
                self.apiclient,
                networkid=networkid,
                services=self.services["icmprule"],
                traffictype='Egress')
        except Exception as e:
            self.fail(
                "Failed while creating Network ACL rule\
                for network %s with error %s" %
                (networkid, e))
        return ingressAcl, egressAcl

    def CheckIngressEgressConnectivityofVM(self, virtual_machine, ipaddress,
                                           port=22):
        try:
            ssh = SshClient(
                ipaddress,
                port,
                virtual_machine.username,
                virtual_machine.password
            )
            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("SSH Access failed for vm %s with IP address %s: %s" %
                      (virtual_machine.id, ipaddress, e))
        result = str(res)
        self.assertEqual(
            result.count("1 received"),
            1,
            "Ping to outside world from VM should be successful")
        return

    def VerifyNetworkCleanup(self, networkid):
        """Verify that network is cleaned up"""

        networks = Network.list(self.apiclient, id=networkid)
        self.assertEqual(
            validateList(networks)[0],
            FAIL,
            "networks list should be empty, it is %s" %
            networks)
        return

    def VerifyVpcCleanup(self, vpcid):
        """Verify that VPC is cleaned up"""
        vpcs = VPC.list(self.apiclient, id=vpcid)
        self.assertEqual(
            validateList(vpcs)[0],
            FAIL,
            "VPC list should be empty, it is %s" %
            vpcs)
        return

    def VerifyVirtualMachineCleanup(self, vmid):
        """Verify that virtual machine is cleaned up"""
        vms = VirtualMachine.list(self.apiclient, id=vmid)
        self.assertEqual(
            validateList(vms)[0],
            FAIL,
            "vms list should be empty, it is %s" %
            vms)
        return

    def VerifyAclRuleCleanup(self, aclRuleId):
        """Verify that network ACL rule is cleaned up"""
        networkAcls = NetworkACL.list(self.apiclient, id=aclRuleId)
        self.assertEqual(
            validateList(networkAcls)[0],
            FAIL,
            "networkAcls list should be empty, it is %s" %
            networkAcls)
        return

    @data("delete", "restart")
    @attr(tags=["advanced"])
    def test_vpc_network_life_cycle(self, value):
        # steps
        # 1. Create account and create VPC network in the account
        # 2. Create two persistent networks within this VPC
        # 3. Restart/delete VPC network

        # Validations
        # 1. In case of Restart operation, restart should be successful
        #    and persistent networks should be back in persistent state
        # 2. In case of Delete operation, VR servicing the VPC should get
        # destroyed and sourceNAT ip should get released

        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   account.name)
        vpc = VPC.create(self.apiclient, self.services["vpc"],
                         vpcofferingid=self.vpc_off.id, zoneid=self.zone.id,
                         account=account.name, domainid=account.domainid)
        vpcs = VPC.list(self.apiclient, id=vpc.id)
        self.assertEqual(
            validateList(vpcs)[0],
            PASS,
            "VPC list validation failed, vpc list is %s" %
            vpcs)

        VpcRouters = Router.list(self.apiclient, vpcid=vpc.id, listall=True)
        self.assertEqual(
            validateList(VpcRouters)[0],
            PASS,
            "VpcRouters list validation failed, list is %s" %
            VpcRouters)
        vpcrouter = VpcRouters[0]

        publicipaddresses = PublicIPAddress.list(
            self.apiclient,
            vpcid=vpc.id,
            listall=True)
        self.assertEqual(
            validateList(publicipaddresses)[0],
            PASS,
            "Public IP Addresses list validation failed, list is %s" %
            publicipaddresses)

        persistent_network_1 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.persistent_network_offering_NoLB.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0")
        response = verifyNetworkState(
            self.apiclient,
            persistent_network_1.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            persistent_network_1.vlan,
            "vlan must not be null for persistent network %s" %
            persistent_network_1.id)

        persistent_network_2 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.persistent_network_offering_NoLB.id,
            accountid=account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.2.1",
            netmask="255.255.255.0")
        response = verifyNetworkState(
            self.apiclient,
            persistent_network_2.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            persistent_network_2.vlan,
            "vlan must not be null for persistent network: %s" %
            persistent_network_2.id)

        if value == "restart":
            # Restart VPC
            vpc.restart(self.apiclient)
            response = verifyNetworkState(
                self.apiclient,
                persistent_network_1.id,
                "implemented")
            exceptionOccured = response[0]
            isNetworkInDesiredState = response[1]
            exceptionMessage = response[2]

            if (exceptionOccured or (not isNetworkInDesiredState)):
                self.fail(exceptionMessage)
            response = verifyNetworkState(
                self.apiclient,
                persistent_network_2.id,
                "implemented")
            exceptionOccured = response[0]
            isNetworkInDesiredState = response[1]
            exceptionMessage = response[2]

            if (exceptionOccured or (not isNetworkInDesiredState)):
                self.fail(exceptionMessage)

        elif value == "delete":
            persistent_network_1.delete(self.apiclient)
            persistent_network_2.delete(self.apiclient)
            vpc.delete(self.apiclient)
            vpcs = VPC.list(self.apiclient, id=vpc.id)
            self.assertEqual(
                validateList(vpcs)[0],
                FAIL,
                "vpc list should be empty, list is %s" %
                vpcs)

            # Check if router is deleted or not
            routers = Router.list(self.apiclient, id=vpcrouter.id)
            self.assertEqual(
                validateList(routers)[0],
                FAIL,
                "routers list should be empty, it is %s" %
                routers)

            # Check if source nat IP address is released
            ipaddresses = PublicIPAddress.list(
                self.apiclient,
                id=publicipaddresses[0].id)
            self.assertEqual(
                validateList(ipaddresses)[0],
                FAIL,
                "public ip addresses list should be empty, list is %s" %
                ipaddresses)
        return

    @attr(tags=["advanced"])
    def test_vpc_force_delete_domain(self):
        # steps
        # 1. Create account and create VPC network in the account
        # 2. Create two persistent networks within this VPC
        # 3. Restart/delete VPC network

        # Validations
        # 1. In case of Restart operation, restart should be successful
        #    and persistent networks should be back in persistent state
        # 2. In case of Delete operation, VR servicing the VPC should
        #    get destroyed and sourceNAT ip should get released

        child_domain = Domain.create(self.apiclient,
                                     services=self.services["domain"],
                                     parentdomainid=self.domain.id)

        try:
            account_1 = Account.create(
                self.apiclient, self.services["account"],
                domainid=child_domain.id
            )
            account_2 = Account.create(
                self.apiclient, self.services["account"],
                domainid=child_domain.id
            )

            self.services["vpc"]["cidr"] = "10.1.1.1/16"
            vpc_1 = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=account_1.name,
                domainid=account_1.domainid)
            vpcs = VPC.list(self.apiclient, id=vpc_1.id)
            self.assertEqual(
                validateList(vpcs)[0],
                PASS,
                "VPC list validation failed, vpc list is %s" %
                vpcs)

            vpc_2 = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=account_2.name,
                domainid=account_2.domainid)
            vpcs = VPC.list(self.apiclient, id=vpc_2.id)
            self.assertEqual(
                validateList(vpcs)[0],
                PASS,
                "VPC list validation failed, vpc list is %s" %
                vpcs)

            persistent_network_1 = Network.create(
                self.api_client, self.services["isolated_network"],
                networkofferingid=self.persistent_network_offering_NoLB.id,
                accountid=account_1.name, domainid=account_1.domainid,
                zoneid=self.zone.id, vpcid=vpc_1.id, gateway="10.1.1.1",
                netmask="255.255.255.0")

            response = verifyNetworkState(self.apiclient,
                                          persistent_network_1.id,
                                          "implemented"
                                          )
            exceptionOccured = response[0]
            isNetworkInDesiredState = response[1]
            exceptionMessage = response[2]

            if (exceptionOccured or (not isNetworkInDesiredState)):
                raise Exception(exceptionMessage)
            self.assertIsNotNone(
                persistent_network_1.vlan,
                "vlan must not be null for persistent network %s" %
                persistent_network_1.id)

            persistent_network_2 = Network.create(
                self.api_client, self.services["isolated_network"],
                networkofferingid=self.persistent_network_offering_NoLB.id,
                accountid=account_2.name, domainid=account_2.domainid,
                zoneid=self.zone.id, vpcid=vpc_2.id, gateway="10.1.1.1",
                netmask="255.255.255.0")
            response = verifyNetworkState(
                self.apiclient,
                persistent_network_2.id,
                "implemented")
            exceptionOccured = response[0]
            isNetworkInDesiredState = response[1]
            exceptionMessage = response[2]

            if (exceptionOccured or (not isNetworkInDesiredState)):
                raise Exception(exceptionMessage)
            self.assertIsNotNone(
                persistent_network_2.vlan,
                "vlan must not be null for persistent network: %s" %
                persistent_network_2.id)

        except Exception as e:
            self.cleanup.append(account_1)
            self.cleanup.append(account_2)
            self.cleanup.append(child_domain)
            self.fail(e)

        # Force delete domain
        child_domain.delete(self.apiclient, cleanup=True)

        self.debug("Waiting for account.cleanup.interval" +
                   " to cleanup any remaining resouces")
        # Sleep 3*account.gc to ensure that all resources are deleted
        wait_for_cleanup(self.apiclient, ["account.cleanup.interval"] * 3)

        with self.assertRaises(Exception):
            Domain.list(self.apiclient, id=child_domain.id)

        with self.assertRaises(Exception):
            Account.list(
                self.apiclient, name=account_1.name,
                domainid=account_1.domainid, listall=True
            )

        with self.assertRaises(Exception):
            Account.list(
                self.apiclient, name=account_2.name,
                domainid=account_2.domainid, listall=True
            )

        self.VerifyVpcCleanup(vpc_1.id)
        self.VerifyVpcCleanup(vpc_2.id)
        self.VerifyNetworkCleanup(persistent_network_1.id)
        self.VerifyNetworkCleanup(persistent_network_2.id)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_vpc_delete_account(self):
        # steps
        # 1. Create account and create VPC network in the account
        # 2. Create two persistent networks within this VPC
        # 3. Restart/delete VPC network

        # Validations
        # 1. In case of Restart operation, restart should be successful and
        #    persistent networks should be back in persistent state
        # 2. In case of Delete operation, VR servicing the VPC should get
        # destroyed and sourceNAT ip should get released

        try:
            # Create Account
            account = Account.create(
                self.apiclient,
                self.services["account"],
                domainid=self.domain.id)

            # Create VPC
            self.services["vpc"]["cidr"] = "10.1.1.1/16"
            vpc = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=account.name,
                domainid=account.domainid)
            vpcs = VPC.list(self.apiclient, id=vpc.id)
            self.assertEqual(
                validateList(vpcs)[0],
                PASS,
                "VPC list validation failed, vpc list is %s" %
                vpcs)

            # Create Persistent Networks as tiers of VPC
            persistent_network_1 = Network.create(
                self.api_client,
                self.services["isolated_network"],
                networkofferingid=self.persistent_network_offering_NoLB.id,
                accountid=account.name,
                domainid=account.domainid,
                zoneid=self.zone.id,
                vpcid=vpc.id,
                gateway="10.1.1.1",
                netmask="255.255.255.0")
            response = verifyNetworkState(
                self.apiclient,
                persistent_network_1.id,
                "implemented")
            exceptionOccured = response[0]
            isNetworkInDesiredState = response[1]
            exceptionMessage = response[2]

            if (exceptionOccured or (not isNetworkInDesiredState)):
                raise Exception(exceptionMessage)
            self.assertIsNotNone(
                persistent_network_1.vlan,
                "vlan must not be null for persistent network %s" %
                persistent_network_1.id)

            persistent_network_2 = Network.create(
                self.api_client,
                self.services["isolated_network"],
                networkofferingid=self.persistent_network_offering_LB.id,
                accountid=account.name,
                domainid=account.domainid,
                zoneid=self.zone.id,
                vpcid=vpc.id,
                gateway="10.1.2.1",
                netmask="255.255.255.0")
            response = verifyNetworkState(
                self.apiclient,
                persistent_network_2.id,
                "implemented")
            exceptionOccured = response[0]
            isNetworkInDesiredState = response[1]
            exceptionMessage = response[2]

            if (exceptionOccured or (not isNetworkInDesiredState)):
                raise Exception(exceptionMessage)
            self.assertIsNotNone(
                persistent_network_2.vlan,
                "vlan must not be null for persistent network: %s" %
                persistent_network_2.id)

            # Deploy VMs in above networks (VM1, VM2 in network1 and VM3, VM4
            # in network2)
            virtual_machine_1 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_1.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            virtual_machine_2 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_1.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            virtual_machine_3 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_2.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            virtual_machine_4 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_2.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            # Associate IP addresses to persistent networks
            ipaddress_1 = self.GetAssociatedIpForNetwork(
                persistent_network_1.id,
                vpcid=vpc.id,
                account=account)
            ipaddress_2 = self.GetAssociatedIpForNetwork(
                persistent_network_1.id,
                vpcid=vpc.id,
                account=account)
            ipaddress_3 = self.GetAssociatedIpForNetwork(
                persistent_network_2.id,
                vpcid=vpc.id,
                account=account)

            # Create NAT rule for VM 1
            NATRule.create(
                self.api_client,
                virtual_machine_1,
                self.services["natrule"],
                ipaddressid=ipaddress_1.ipaddress.id,
                networkid=persistent_network_1.id)

            # Create Static NAT rule for VM 2
            StaticNATRule.enable(
                self.apiclient,
                ipaddressid=ipaddress_2.ipaddress.id,
                virtualmachineid=virtual_machine_2.id,
                networkid=persistent_network_1.id)

            # Create load balancer rule for ipaddress3 and assign to VM3 and
            # VM4
            lb_rule = LoadBalancerRule.create(
                self.apiclient,
                self.services["lbrule"],
                ipaddressid=ipaddress_3.ipaddress.id,
                accountid=account.name,
                networkid=persistent_network_2.id,
                domainid=account.domainid)
            lb_rule.assign(
                self.api_client, [
                    virtual_machine_3, virtual_machine_4])

            # Create network ACL for both ther persistent networks (tiers of
            # VPC)
            ingressAclNetwork1, egressAclNetwork1 =\
                self.CreateIngressEgressNetworkACLForNetwork(
                    persistent_network_1.id
                )
            ingressAclNetwork2, egressAclNetwork2 =\
                self.CreateIngressEgressNetworkACLForNetwork(
                    persistent_network_2.id
                )

            # CLOUDSTACK-8451 needs to be fixed in order to work
            self.CheckIngressEgressConnectivityofVM(
                virtual_machine_1,
                ipaddress_1.ipaddress.ipaddress)
            self.CheckIngressEgressConnectivityofVM(
                virtual_machine_2,
                ipaddress_2.ipaddress.ipaddress)
            self.CheckIngressEgressConnectivityofVM(
                virtual_machine_3,
                ipaddress_3.ipaddress.ipaddress,
                port=self.services["lbrule"]["publicport"])
            self.CheckIngressEgressConnectivityofVM(
                virtual_machine_4,
                ipaddress_3.ipaddress.ipaddress,
                port=self.services["lbrule"]["publicport"])
        except Exception as e:
            self.cleanup.append(account)
            self.fail(e)

        # Delete account
        account.delete(self.apiclient)

        # Verify all the resources owned by the account are deleted
        self.debug("Waiting for account.cleanup.interval" +
                   " to cleanup any remaining resouces")
        # Sleep 3*account.gc to ensure that all resources are deleted
        wait_for_cleanup(self.apiclient, ["account.cleanup.interval"] * 3)

        self.VerifyVpcCleanup(vpc.id)
        self.VerifyNetworkCleanup(persistent_network_1.id)
        self.VerifyNetworkCleanup(persistent_network_2.id)
        self.VerifyVirtualMachineCleanup(virtual_machine_1.id)
        self.VerifyVirtualMachineCleanup(virtual_machine_2.id)
        self.VerifyVirtualMachineCleanup(virtual_machine_3.id)
        self.VerifyVirtualMachineCleanup(virtual_machine_4.id)
        self.VerifyAclRuleCleanup(ingressAclNetwork1.id)
        self.VerifyAclRuleCleanup(egressAclNetwork1.id)
        self.VerifyAclRuleCleanup(ingressAclNetwork2.id)
        self.VerifyAclRuleCleanup(egressAclNetwork2.id)
        return

    @unittest.skip("WIP")
    @attr(tags=["advanced"])
    def test_vpc_private_gateway_static_route(self):
        # steps
        # 1. Create account and create VPC network in the account
        # 2. Create two persistent networks within this VPC
        # 3. Restart/delete VPC network

        # Validations
        # 1. In case of Restart operation, restart should be successful
        #    and persistent networks should be back in persistent state
        # 2. In case of Delete operation, VR servicing the VPC should get
        # destroyed and sourceNAT ip should get released

        # Create Account
        account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id)
        self.cleanup.append(account)

        # Create VPC
        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        vpc = VPC.create(self.apiclient, self.services["vpc"],
                         vpcofferingid=self.vpc_off.id, zoneid=self.zone.id,
                         account=account.name, domainid=account.domainid)
        vpcs = VPC.list(self.apiclient, id=vpc.id)
        self.assertEqual(
            validateList(vpcs)[0],
            PASS,
            "VPC list validation failed, vpc list is %s" %
            vpcs)

        # Create Persistent Networks as tiers of VPC
        persistent_network_1 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.persistent_network_offering_NoLB.id,
            accountid=account.name,
            domainid=account.domainid,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0")
        response = verifyNetworkState(
            self.apiclient,
            persistent_network_1.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            persistent_network_1.vlan,
            "vlan must not be null for persistent network %s" %
            persistent_network_1.id)

        persistent_network_2 = Network.create(
            self.api_client,
            self.services["isolated_network"],
            networkofferingid=self.persistent_network_offering_LB.id,
            accountid=account.name,
            domainid=account.domainid,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.2.1",
            netmask="255.255.255.0")
        response = verifyNetworkState(
            self.apiclient,
            persistent_network_2.id,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            persistent_network_2.vlan,
            "vlan must not be null for persistent network: %s" %
            persistent_network_2.id)

        # Deploy VMs in above networks (VM1, VM2 in network1 and VM3, VM4 in
        # network2)
        try:
            virtual_machine_1 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_1.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            virtual_machine_2 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_1.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            virtual_machine_3 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_2.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)

            virtual_machine_4 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    persistent_network_2.id],
                serviceofferingid=self.service_offering.id,
                accountid=account.name,
                domainid=self.domain.id)
        except Exception as e:
            self.fail("vm creation failed: %s" % e)

        # Associate IP addresses to persistent networks
        ipaddress_1 = self.GetAssociatedIpForNetwork(
            persistent_network_1.id,
            vpcid=vpc.id,
            account=account)
        ipaddress_2 = self.GetAssociatedIpForNetwork(
            persistent_network_1.id,
            vpcid=vpc.id,
            account=account)
        ipaddress_3 = self.GetAssociatedIpForNetwork(
            persistent_network_2.id,
            vpcid=vpc.id,
            account=account)

        # Create NAT rule for VM 1
        NATRule.create(
            self.api_client,
            virtual_machine_1,
            self.services["natrule"],
            ipaddressid=ipaddress_1.ipaddress.id,
            networkid=persistent_network_1.id)

        # Create Static NAT rule for VM 2
        StaticNATRule.enable(
            self.apiclient,
            ipaddressid=ipaddress_2.ipaddress.id,
            virtualmachineid=virtual_machine_2.id,
            networkid=persistent_network_1.id)

        # Create load balancer rule for ipaddress3 and assign to VM3 and VM4
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=ipaddress_3.ipaddress.id,
            accountid=account.name,
            networkid=persistent_network_2.id,
            domainid=account.domainid)
        lb_rule.assign(self.api_client, [virtual_machine_3, virtual_machine_4])

        # Create network ACL for both ther persistent networks (tiers of VPC)
        ingressAclNetwork1, egressAclNetwork1 =\
            self.CreateIngressEgressNetworkACLForNetwork(
                persistent_network_1.id
            )
        ingressAclNetwork2, egressAclNetwork2 =\
            self.CreateIngressEgressNetworkACLForNetwork(
                persistent_network_2.id
            )

        """private_gateway = PrivateGateway.create(
            self.apiclient,gateway='10.1.4.1',
            ipaddress='10.1.4.100',
            netmask='255.255.255.0',
            vlan=679,
            vpcid=vpc.id)

        gateways = PrivateGateway.list(
            self.apiclient,
            id=private_gateway.id,
            listall=True)
        self.assertEqual(
            validateList(gateways)[0],
            PASS,
            "gateways list validation failed, list is %s" % gateways
            )

        static_route = StaticRoute.create(
            self.apiclient,
            cidr='11.1.1.1/24',
            gatewayid=private_gateway.id)
        static_routes = StaticRoute.list(
            self.apiclient,
            id=static_route.id,
            listall=True)
        self.assertEqual(
            validateList(static_routes)[0],
            PASS,
            "static routes list validation failed, list is %s"
            % static_routes)"""

        self.CheckIngressEgressConnectivityofVM(
            virtual_machine_1,
            ipaddress_1.ipaddress.ipaddress)
        self.CheckIngressEgressConnectivityofVM(
            virtual_machine_2,
            ipaddress_2.ipaddress.ipaddress)
        self.CheckIngressEgressConnectivityofVM(
            virtual_machine_3,
            ipaddress_3.ipaddress.ipaddress)
        """self.CheckIngressEgressConnectivityofVM(virtual_machine_4,
        ipaddress_3.ipaddress.ipaddress)"""

        vpc.restart(self.apiclient)

        self.CheckIngressEgressConnectivityofVM(
            virtual_machine_1,
            ipaddress_1.ipaddress.ipaddress)
        self.CheckIngressEgressConnectivityofVM(
            virtual_machine_2,
            ipaddress_2.ipaddress.ipaddress)
        self.CheckIngressEgressConnectivityofVM(
            virtual_machine_3,
            ipaddress_3.ipaddress.ipaddress)
        """self.CheckIngressEgressConnectivityofVM(virtual_machine_4,
        ipaddress_3.ipaddress.ipaddress)"""
        return
