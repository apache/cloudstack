#!/usr/bin/env python
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

import requests
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (
    PhysicalNetwork,
    NetworkOffering,
    NiciraNvp,
    ServiceOffering,
    NATRule,
    PublicIPAddress,
    Network,
    VirtualMachine
)
from marvin.lib.common import (
    get_domain,
    get_zone,
    get_template,
    list_routers,
    list_hosts,
    findSuitableHostForMigration
)
from nose.plugins.attrib import attr
from marvin.codes import (FAILED, PASS)
import time
import logging

class TestNiciraContoller(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        test_case = super(TestNiciraContoller, cls)

        test_client    = test_case.getClsTestClient()
        cls.config     = test_case.getClsConfig()
        cls.api_client = test_client.getApiClient()

        cls.physical_networks = cls.config.zones[0].physical_networks
        cls.nicira_hosts      = cls.config.niciraNvp.hosts

        cls.physical_network_id = cls.get_nicira_enabled_physical_network_id(cls.physical_networks)

        cls.network_offerring_services = {
            'name':              'NiciraEnabledNetwork',
            'displaytext':       'NiciraEnabledNetwork',
            'guestiptype':       'Isolated',
            'supportedservices': 'SourceNat,Dhcp,Dns,Firewall,PortForwarding,Connectivity',
            'traffictype':       'GUEST',
            'availability':      'Optional',
            'serviceProviderList': {
                    'SourceNat':      'VirtualRouter',
                    'Dhcp':           'VirtualRouter',
                    'Dns':            'VirtualRouter',
                    'Firewall':       'VirtualRouter',
                    'PortForwarding': 'VirtualRouter',
                    'Connectivity':   'NiciraNvp'
            }
        }

        cls.network_offering = NetworkOffering.create(cls.api_client, cls.network_offerring_services)
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls.nicira_credentials = {
            'username': 'admin',
            'password': 'admin'
        }

        cls.nicira_master_controller = cls.determine_master_controller(
            cls.nicira_hosts,
            cls.nicira_credentials
        )

        cls.transport_zone_uuid = cls.get_transport_zone_from_controller(
            cls.nicira_master_controller,
            cls.nicira_credentials
        )

        cls.domain = get_domain(cls.api_client)
        cls.zone   = get_zone(cls.api_client, test_client.getZoneForTests())

        template = get_template(
            cls.api_client,
            cls.zone.id
        )
        if template == FAILED:
            raise Exception("get_template() failed to return template with description %s" % cls.services['ostype'])

        cls.vm_services = {
            'mode': cls.zone.networktype,
            'small': {
                'zoneid':      cls.zone.id,
                'template':    template.id,
                'displayname': 'testserver',
                'username':    cls.config.zones[0].pods[0].clusters[0].hosts[0].username,
                'password':    cls.config.zones[0].pods[0].clusters[0].hosts[0].password,
                'ssh_port':    22,
                'hypervisor':  cls.config.zones[0].pods[0].clusters[0].hypervisor,
                'privateport': 22,
                'publicport':  22,
                'protocol':    'TCP',
            },
            'service_offerings': {
                'tiny': {
                    'name':        'Tiny Instance',
                    'displaytext': 'Tiny Instance',
                    'cpunumber':   1,
                    'cpuspeed':    100,
                    'memory':      64,
                }
            }
        }

        if cls.zone.localstorageenabled == True:
            cls.vm_services['service_offerings']['tiny']['storagetype'] = 'local'

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.vm_services['service_offerings']['tiny']
        )

        cls.cleanup = [
            cls.network_offering,
            cls.service_offering
        ]

        cls.logger = logging.getLogger('TestNiciraContoller')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)


    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, reversed(cls.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during class cleanup : %s" % e)

    def setUp(self):
        self.test_cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.api_client, reversed(self.test_cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during test cleanup : %s" % e)


    @classmethod
    def determine_master_controller(cls, hosts, credentials):
        for host in hosts:
            r1 = requests.post("https://%s/ws.v1/login" % host, credentials, verify=False)
            r2 = requests.get("https://%s/ws.v1/control-cluster/status" % host, verify=False, cookies=r1.cookies)
            status_code = r2.status_code
            if status_code == 401:
                continue
            elif status_code == 200:
                return host
        raise Exception("None of the supplied hosts (%s) is a Nicira controller" % hosts)


    @classmethod
    def get_transport_zone_from_controller(cls, controller_host, credentials):
        r1 = requests.post("https://%s/ws.v1/login" % controller_host, credentials, verify=False)
        r2 = requests.get("https://%s/ws.v1/transport-zone" % controller_host, verify=False, cookies=r1.cookies)
        status_code = r2.status_code
        if status_code == 200:
            list_transport_zone_response = r2.json()
            result_count = list_transport_zone_response['result_count']
            if result_count == 0:
                raise Exception('Nicira controller did not return any Transport Zones')
            elif result_count > 1:
                self.logger.debug("Nicira controller returned %s Transport Zones, picking first one" % resultCount)
            transport_zone_api_url = list_transport_zone_response['results'][0]['_href']
            r3 = requests.get(
                "https://%s%s" % (controller_host, transport_zone_api_url),
                verify=False,
                cookies=r1.cookies
            )
            return r3.json()['uuid']
        else:
            raise Exception("Unexpected response from Nicira controller. Status code = %s, content = %s" % status_code)


    @classmethod
    def get_nicira_enabled_physical_network_id(cls, physical_networks):
        nicira_physical_network_name = None
        for physical_network in physical_networks:
            for provider in physical_network.providers:
                if provider.name == 'NiciraNvp':
                    nicira_physical_network_name = physical_network.name
        if nicira_physical_network_name is None:
            raise Exception('Did not find a Nicira enabled physical network in configuration')
        return PhysicalNetwork.list(cls.api_client, name=nicira_physical_network_name)[0].id


    def determine_slave_conroller(self, hosts, master_controller):
        slaves = [ s for s in hosts if s != master_controller ]
        if len(slaves) > 0:
            return slaves[0]
        else:
            raise Exception("None of the supplied hosts (%s) is a Nicira slave" % hosts)


    def add_nicira_device(self, hostname):
        nicira_device = NiciraNvp.add(
            self.api_client,
            None,
            self.physical_network_id,
            hostname=hostname,
            username=self.nicira_credentials['username'],
            password=self.nicira_credentials['password'],
            transportzoneuuid=self.transport_zone_uuid)
        self.test_cleanup.append(nicira_device)


    def create_guest_network(self):
        network_services = {
            'name'            : 'nicira_enabled_network',
            'displaytext'     : 'nicira_enabled_network',
            'zoneid'          : self.zone.id,
            'networkoffering' : self.network_offering.id
        }
        network = Network.create(
            self.api_client,
            network_services,
            accountid='admin',
            domainid=self.domain.id,
        )
        self.test_cleanup.append(network)
        return network


    def create_virtual_machine(self, network):
        virtual_machine = VirtualMachine.create(
            self.api_client,
            self.vm_services['small'],
            accountid='admin',
            domainid=self.domain.id,
            serviceofferingid=self.service_offering.id,
            networkids=[network.id],
            mode=self.vm_services['mode']
        )
        self.test_cleanup.append(virtual_machine)
        return virtual_machine


    def get_routers_for_network(self, network):
        return list_routers(
            self.api_client,
            account='admin',
            domainid=self.domain.id,
            networkid=network.id
        )


    def get_hosts(self):
        return list_hosts(
            self.api_client,
            account='admin',
            domainid=self.domain.id
        )


    def get_master_router(self, routers):
        master = filter(lambda r: r.redundantstate == 'MASTER', routers)
        self.logger.debug("Found %s master router(s): %s" % (master.size(), master))
        return master[0]


    def distribute_vm_and_routers_by_hosts(self, virtual_machine, routers):
        if len(routers) > 1:
            router = self.get_router(routers)
            self.logger.debug("Master Router VM is %s" % router)
        else:
            router = routers[0]

        if router.hostid == virtual_machine.hostid:
            self.logger.debug("Master Router VM is on the same host as VM")
            host = findSuitableHostForMigration(self.api_client, router.id)
            if host is not None:
                router.migrate(self.api_client, host)
                self.logger.debug("Migrated Master Router VM to host %s" % host)
            else:
                self.fail('No suitable host to migrate Master Router VM to')
        else:
            self.logger.debug("Master Router VM is not on the same host as VM: %s, %s" % (router.hostid, virtual_machine.hostid))


    def acquire_publicip(self, network):
        self.logger.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(
            self.api_client,
            accountid='admin',
            zoneid=self.zone.id,
            domainid=self.domain.id,
            networkid=network.id
        )
        self.logger.debug("Associated %s with network %s" % (public_ip.ipaddress.ipaddress, network.id))
        self.test_cleanup.append(public_ip)
        return public_ip


    def create_natrule(self, vm, public_ip, network):
        self.logger.debug("Creating NAT rule in network for vm with public IP")
        nat_rule = NATRule.create(
            self.api_client,
            vm,
            self.vm_services['small'],
            ipaddressid=public_ip.ipaddress.id,
            openfirewall=True,
            networkid=network.id
        )
        self.test_cleanup.append(nat_rule)
        return nat_rule


    @attr(tags = ["advanced", "smoke", "nicira"], required_hardware="true")
    def test_01_nicira_controller(self):
        self.add_nicira_device(self.nicira_master_controller)

        network         = self.create_guest_network()
        virtual_machine = self.create_virtual_machine(network)

        list_vm_response = VirtualMachine.list(self.api_client, id=virtual_machine.id)
        self.logger.debug("Verify listVirtualMachines response for virtual machine: %s" % virtual_machine.id)

        self.assertEqual(isinstance(list_vm_response, list), True, 'Response did not return a valid list')
        self.assertNotEqual(len(list_vm_response), 0, 'List of VMs is empty')

        vm_response = list_vm_response[0]
        self.assertEqual(vm_response.id, virtual_machine.id, 'Virtual machine in response does not match request')
        self.assertEqual(vm_response.state, 'Running', 'VM is not in Running state')


    @attr(tags = ["advanced", "smoke", "nicira"], required_hardware="true")
    def test_02_nicira_controller_redirect(self):
        """
            Nicira clusters will redirect clients (in this case ACS) to the master node.
            This test assumes that a Nicira cluster is present and configured properly, and
            that it has at least two controller nodes. The test will check that ASC follows
            redirects by:
                - adding a Nicira Nvp device that points to one of the cluster's slave controllers,
                - create a VM in a Nicira backed network
            If all is well, no matter what controller is specified (slaves or master), the vm (and respective router VM)
            should be created without issues.
        """
        nicira_slave = self.determine_slave_conroller(self.nicira_hosts, self.nicira_master_controller)
        self.logger.debug("Nicira slave controller is: %s " % nicira_slave)

        self.add_nicira_device(nicira_slave)

        network         = self.create_guest_network()
        virtual_machine = self.create_virtual_machine(network)

        list_vm_response = VirtualMachine.list(self.api_client, id=virtual_machine.id)
        self.logger.debug("Verify listVirtualMachines response for virtual machine: %s" % virtual_machine.id)

        self.assertEqual(isinstance(list_vm_response, list), True, 'Response did not return a valid list')
        self.assertNotEqual(len(list_vm_response), 0, 'List of VMs is empty')

        vm_response = list_vm_response[0]
        self.assertEqual(vm_response.id, virtual_machine.id, 'Virtual machine in response does not match request')
        self.assertEqual(vm_response.state, 'Running', 'VM is not in Running state')


    @attr(tags = ["advanced", "smoke", "nicira"], required_hardware="true")
    def test_03_nicira_tunnel_guest_network(self):
        self.add_nicira_device(self.nicira_master_controller)
        network         = self.create_guest_network()
        virtual_machine = self.create_virtual_machine(network)
        public_ip       = self.acquire_publicip(network)
        nat_rule        = self.create_natrule(virtual_machine, public_ip, network)

        list_vm_response = VirtualMachine.list(self.api_client, id=virtual_machine.id)
        self.logger.debug("Verify listVirtualMachines response for virtual machine: %s" % virtual_machine.id)

        self.assertEqual(isinstance(list_vm_response, list), True, 'Response did not return a valid list')
        self.assertNotEqual(len(list_vm_response), 0, 'List of VMs is empty')

        vm_response = list_vm_response[0]
        self.assertEqual(vm_response.id, virtual_machine.id, 'Virtual machine in response does not match request')
        self.assertEqual(vm_response.state, 'Running', 'VM is not in Running state')

        routers = self.get_routers_for_network(network)

        self.distribute_vm_and_routers_by_hosts(virtual_machine, routers)

        ssh_command = 'ping -c 3 google.com'
        result = 'failed'
        try:
            self.logger.debug("SSH into VM: %s" % public_ip.ipaddress.ipaddress)
            ssh = virtual_machine.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
            self.logger.debug('Ping to google.com from VM')
            result = str(ssh.execute(ssh_command))
            self.logger.debug("SSH result: %s; COUNT is ==> %s" % (result, result.count("3 packets received")))
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % (vmObj.get_ip(), e))

        self.assertEqual(result.count('3 packets received'), 1, 'Ping to outside world from VM should be successful')
