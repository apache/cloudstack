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

from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (cleanup_resources,
                              validateList,
                              get_hypervisor_type, get_process_status)
from marvin.lib.base import (Account,
                             Cluster,
                             Configurations,
                             Host,
                             VPC,
                             VirtualMachine,
                             Network,
                             Router,
                             ServiceOffering,
                             NetworkOffering)
from marvin.lib.common import (get_zone,
                               get_template,
                               verifyNetworkState,
                               wait_for_cleanup, list_routers, list_hosts)
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient
from distutils.util import strtobool
from pyVmomi import vim, vmodl
from marvin.lib.vcenter import Vcenter
import logging

logger = logging.getLogger('TestPesistentNetwork')
stream_handler = logging.StreamHandler()
logger.setLevel(logging.DEBUG)
logger.addHandler(stream_handler)


class TestL2PersistentNetworks(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestL2PersistentNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        isKVM = cls.hypervisor.lower() in ["kvm"]
        isOVSEnabled = False
        hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        if isKVM :
            # Test only if all the hosts use OVS
            grepCmd = 'grep "network.bridge.type=openvswitch" /etc/cloudstack/agent/agent.properties'
            hosts = list_hosts(cls.api_client, type='Routing', hypervisor='kvm')
            for host in hosts :
                if len(SshClient(host.ipaddress, port=22, user=hostConfig["username"],
                    passwd=hostConfig["password"]).execute(grepCmd)) != 0 :
                        isOVSEnabled = True
                        break
        if isKVM and isOVSEnabled :
            cls.skipTest(cls, "KVM with OVS doesn't support persistent networks, skipping")

        # Fill services from the external config file
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        # Get Zone and templates
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
        cls.l2_persistent_network_offering = cls.create_network_offering("nw_off_L2_persistent")
        cls.isolated_persistent_network_offering = cls.create_network_offering("nw_off_isolated_persistent")

        # network will be deleted as part of account cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.isolated_persistent_network_offering,
            cls.l2_persistent_network_offering]
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
        self.hypervisor = self.testClient.getHypervisorInfo()
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

    @classmethod
    def is_ssh_enabled(cls):
        conf = Configurations.list(cls.apiclient, name="kvm.ssh.to.agent")
        if not conf:
            return False
        else:
            return bool(strtobool(conf[0].value)) if conf[0].value else False

    @classmethod
    def create_network_offering(cls, network_offering_type):
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

    def get_ssh_client(self, ip, username, password, retries=10):
        """ Setup ssh client connection and return connection """
        try:
            ssh_client = SshClient(ip, 22, username, password, retries)
        except Exception as e:
            raise unittest.SkipTest("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to ip=%s" % ip)

        return ssh_client

    def list_all_hosts_in_zone(self, zone_id):
        hosts = Host.list(
            self.apiclient,
            type='Routing',
            resourcestate='Enabled',
            state='Up',
            zoneid=zone_id
        )
        return hosts

    '''
    Verifies creation of bridge on KVM host
    '''
    def verify_bridge_creation(self, host, vlan_id):
        username = self.hostConfig["username"]
        password = self.hostConfig["password"]
        try:
            ssh_client = self.get_ssh_client(host.ipaddress, username, password)
            res = ssh_client.execute("ip addr | grep breth1-" + str(vlan_id) + " > /dev/null 2>&1; echo $?")
            return res[0]
        except Exception as e:
            self.fail(e)

    '''
    Gets all port groups on the host
    '''
    def capture_host_portgroups(self, host):
        host_portgroups = []
        for portgroup in host.config.network.portgroup:
            host_portgroups.append(portgroup.spec.name)
        return host_portgroups

    '''
    Fetches port group names based on VMware switch type - Distributed Virtual Switch(DVS) and
    Standard Virtual Switch(SVS)
    '''
    def get_port_group_name(self, switch_type, vlan_id, network_rate):
        if switch_type == 'DVS':
            return 'cloud.guest.' + str(vlan_id) + '.' + str(network_rate) + '.1-dvSwitch1'
        elif switch_type == 'SVS':
            return 'cloud.guest.' + str(vlan_id) + '.' + str(network_rate) + '.1-vSwitch1'
        else:
            return None

    '''
    Verifies creation of port group on the Distributed vSwitch or a host in a cluster connected to
    a Standard vSwitch
    '''
    def verify_port_group_creation(self, vlan_id):
        config = self.get_vmware_dc_config(self.zone.id)
        vc_object = Vcenter(config[0][0], config[0][1], 'P@ssword123')
        dvs = vc_object.get_dvswitches()
        port_group_present = False
        if dvs is not None:
            port_group_name = self.get_port_group_name('DVS', vlan_id,
                                                       self.isolated_persistent_network_offering.networkrate)
            port_group_present = port_group_name in dvs[0]['dvswitch']['portgroupNameList']

        else:
            port_group_name = self.get_port_group_name('SVS', vlan_id,
                                                       self.isolated_persistent_network_offering.networkrate)
            hosts = vc_object._get_obj([vim.HostSystem])
            host = hosts[0]['host']
            host_pg = self.capture_host_portgroups(host)
            port_group_present = port_group_name in host_pg
        return port_group_present

    '''
    Fetch vmware datacenter login details
    '''
    def get_vmware_dc_config(self, zone_id):
        zid = self.dbclient.execute("select id from data_center where uuid='%s';" %
                                    zone_id)
        vmware_dc_id = self.dbclient.execute(
            "select vmware_data_center_id from vmware_data_center_zone_map where zone_id='%s';" %
            zid[0])
        vmware_dc_config = self.dbclient.execute(
            "select vcenter_host, username, password from vmware_data_center where id = '%s';" % vmware_dc_id[0])

        return vmware_dc_config

    '''
    Verify VLAN creation on specific host in a cluster
    '''
    def verify_vlan_network_creation(self, host, vlan_id):
        username = self.hostConfig["username"]
        password = self.hostConfig["password"]
        try:
            ssh_client = self.get_ssh_client(host.ipaddress, username, password)
            res = ssh_client.execute(
                "xe vlan-list | grep -x  \"^\s*tag ( RO): \"" + str(vlan_id) + "> /dev/null 2>&1; echo $?")
            return res[0]
        except Exception as e:
            self.fail(e)

    def verify_network_setup_on_host_per_cluster(self, hypervisor, vlan_id):
        clusters = Cluster.list(
            self.apiclient,
            zoneid=self.zone.id,
            allocationstate="Enabled",
            listall=True
        )
        for cluster in clusters:
            hosts = Host.list(self.apiclient,
                              clusterid=cluster.id,
                              type="Routing",
                              state="Up",
                              resourcestate="Enabled")
            host = hosts[0]
            if hypervisor == "xenserver":
                result = self.verify_vlan_network_creation(host, vlan_id)
                self.assertEqual(
                    int(result),
                    0,
                    "Failed to find vlan on host: " + host.name + " in cluster: " + cluster.name)
            if hypervisor == "vmware":
                result = self.verify_port_group_creation(vlan_id)
                self.assertEqual(
                        result,
                        True,
                        "Failed to find port group on hosts of cluster: " + cluster.name)

    '''
    This test verifies that on creation of an Isolated network with network offering with isPersistent flag
    set to true the corresponding network resources are created without having to deploy a VM - VR created
    '''
    @attr(tags=["advanced", "isolated", "persistent", "network"], required_hardware="false")
    def test_01_isolated_persistent_network(self):
        network = Network.create(
            self.apiclient,
            self.services["isolated_network"],
            networkofferingid=self.isolated_persistent_network_offering.id,
            zoneid=self.zone.id)
        self.cleanup.append(network)
        networkVlan = network.vlan
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
            networkVlan,
            "vlan must not be null for persistent network")

        router = Router.list(self.apiclient, networkid=network.id)[0]
        router_host_id = router.hostid
        host = Host.list(self.apiclient, id=router_host_id)[0]
        if host.hypervisor.lower() in "kvm":
            result = self.verify_bridge_creation(host, networkVlan)
            self.assertEqual(
                int(result),
                0,
                "Failed to find bridge on the breth1$-{networkVlan}")
        elif host.hypervisor.lower() in ["xenserver", "vmware"]:
            self.verify_network_setup_on_host_per_cluster(host.hypervisor.lower(), networkVlan)

    '''
    This test verifies that on creation of an L2 network with network offering with isPersistent flag
    set to true the corresponding network resources are created without having to deploy a VM - VR created
    '''
    @attr(tags=["advanced", "l2", "persistent", "network"], required_hardware="false")
    def test_02_L2_persistent_network(self):
        network_vlan = 90
        network = Network.create(
            self.apiclient,
            self.services["l2_network"],
            networkofferingid=self.l2_persistent_network_offering.id,
            zoneid=self.zone.id,
            vlan=network_vlan)
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
            network_vlan,
            "vlan must not be null for persistent network")

        self.validate_persistent_network_resources_created_on_host(network_vlan)

    @attr(tags=["advanced", "l2", "persistent", "network"], required_hardware="false")
    def test_03_deploy_and_destroy_VM_and_verify_network_resources_persist(self):
        network_vlan = 99
        network = Network.create(
            self.apiclient,
            self.services["l2_network"],
            networkofferingid=self.l2_persistent_network_offering.id,
            zoneid=self.zone.id,
            vlan=network_vlan)
        self.cleanup.append(network)
        response = verifyNetworkState(
            self.apiclient,
            network.id,
            "implemented")
        logger.debug(response)
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)
        self.assertIsNotNone(
            network_vlan,
            "vlan must not be null for persistent network")
        try:
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                networkids=[
                    network.id],
                serviceofferingid=self.service_offering.id)

            VirtualMachine.delete(virtual_machine, self.apiclient, expunge=True)

            self.validate_persistent_network_resources_created_on_host(network_vlan)
        except Exception as e:
            self.fail("Exception occurred: %s" % e)
        return


    def validate_persistent_network_resources_created_on_host(self, network_vlan):
        hosts = self.list_all_hosts_in_zone(self.zone.id)
        if self.hypervisor.lower() in "kvm":
            for host in hosts:
                result = self.verify_bridge_creation(host, network_vlan)
                self.assertEqual(
                    int(result),
                    0,
                    "Failed to find bridge on the breth1-" + str(network_vlan))
        elif self.hypervisor.lower() in ["xenserver", "vmware"]:
            self.verify_network_setup_on_host_per_cluster(self.hypervisor.lower(), network_vlan)