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

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              _execute_ssh_command,
                              get_host_credentials,
                              random_gen)
from marvin.lib.base import (PhysicalNetwork,
                             Host,
                             TrafficType,
                             Domain,
                             Network,
                             NetworkOffering,
                             VirtualMachine,
                             ServiceOffering,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines)

import logging

class TestMultipleNetworkCreation(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestMultipleNetworkCreation,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.domain = get_domain(cls.apiclient)
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        cls.logger = logging.getLogger("TestMultipleNetworkCreation")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Disable the zone to create physical networks
        cls.zone.update(
            cls.apiclient,
            allocationstate="Disabled"
        )

        try:
            cls.physical_network = PhysicalNetwork.create(
                cls.apiclient,
                cls.services["l2-network"],
                zoneid=cls.zone.id
            )
            cls._cleanup.append(cls.physical_network)

            cls.physical_network_2 = PhysicalNetwork.create(
                cls.apiclient,
                cls.services["l2-network"],
                zoneid=cls.zone.id
            )
            cls._cleanup.append(cls.physical_network_2)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)

        cls.kvmnetworklabel=None
        try:
            hosts = Host.list(cls.apiclient, type='Routing')
            if isinstance(hosts, list) and len(hosts) > 0:
                host = hosts[0]
            else:
                return
            if host.hypervisor.lower() not in "kvm":
                return
            host.user, host.passwd = get_host_credentials(cls.config, host.ipaddress)
            bridges = _execute_ssh_command(host.ipaddress, 22, host.user, host.passwd, "brctl show |grep cloudbr |awk '{print $1}'")
            existing_physical_networks = PhysicalNetwork.list(cls.apiclient)
            for existing_physical_network in existing_physical_networks:
                trafficTypes = TrafficType.list(
                    cls.apiclient,
                    physicalnetworkid=existing_physical_network.id)
                if trafficTypes is None:
                    continue
                for traffic_type in trafficTypes:
                    if traffic_type.traffictype == "Guest":
                        try:
                            for bridge in bridges:
                                if bridge == str(traffic_type.kvmnetworklabel):
                                    bridges.remove(bridge)
                        except Exception as e:
                            continue

            if bridges is not None and len(bridges) > 0:
                cls.kvmnetworklabel = bridges[0]
            if cls.kvmnetworklabel is not None:
                cls.logger.debug("Found an unused kvmnetworklabel %s" %cls.kvmnetworklabel)
            else:
                cls.logger.debug("Not find an unused kvmnetworklabel")
        except Exception as e:
            return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.zone.update(
                cls.apiclient,
                allocationstate="Enabled"
            )
            # Cleanup resources used
            super(TestMultipleNetworkCreation, cls).tearDownClass()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

        return

    def tearDown(self):
        super(TestMultipleNetworkCreation, self).tearDown()

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_add_traffictype_for_untagged_networks(self):
        """
        Try to add to traffic type Guest to each of the network
        Two physical networks are already created without tags

        1. Try to add traffic type Guest to physcial network
        2. Ensure that it throws exception
        3. Now add the tags to the physical network
        4. Add traffic type Guest to the physical network and it should not throw exception
        5. Do the same above steps for the other physical network

        :return:
        """
        try:
            # Add traffic type
            self.physical_network.addTrafficType(
                self.apiclient,
                type="Guest"
            )

            # If the control comes then there is something wrong with the code.
            # The code should throw exception as there are multiple networks with null tags
            raise Exception("Exception should occur when adding traffic type since tags are null")
        except Exception as e:
            self.logger.info("Exception happened as expected")

        # Now update the network with tags
        self.physical_network.update(
            self.apiclient,
            tags="Guest"
        )

        # try adding traffic type again. it should not throw error
        self.physical_network.addTrafficType(
            self.apiclient,
            type="Guest"
        )

        # Do the same thing for the second network
        try:
            self.physical_network_2.addTrafficType(
                self.apiclient,
                type="Guest"
            )

            # It should throw exception and should not come here
            raise Exception("Exception should occur when adding traffic type since tags are null")
        except Exception as e:
            self.logger.info("Exception happened as expected")

        # Now update the network with tags
        self.physical_network_2.update(
            self.apiclient,
            tags="Guest"
        )

        # try adding traffic type again. it should not throw error
        self.physical_network_2.addTrafficType(
            self.apiclient,
            type="Guest"
        )

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_created_shared_guest_network(self):
        """
        1. Create new physical network
        2. Update the network with tags and traffic type "Guest"
        3. Create a network offering and shared network based on the above physical network
        4. Create a virtual machine using the above created network
        4. Ensure that the traffic type is Guest and vlan is same as the shared network
        :return:
        """
        #1. Create a physical network
        self.physical_network_3 = PhysicalNetwork.create(
            self.apiclient,
            self.services["l2-network"],
            isolationmethods="VLAN",
            zoneid=self.zone.id
        )
        self.cleanup.append(self.physical_network_3)

        # Enable the network
        self.physical_network_3.update(
            self.apiclient,
            tags="guest",
            state="Enabled"
        )

        #2. try adding traffic type Guest
        TrafficType.add(
            self.apiclient,
            physicalnetworkid=self.physical_network_3.id,
            kvmnetworklabel=self.kvmnetworklabel,
            traffictype="Guest"
        )

        # Create network offering
        self.services["network_offering_shared"]["supportedservices"] = ""
        self.services["network_offering_shared"]["serviceProviderList"] = {}
        self.services["network_offering_shared"]["tags"] = "guest"
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering_shared"]
        )
        self.cleanup.append(self.network_offering)

        # Enable network offering
        self.network_offering.update(
            self.apiclient,
            state='Enabled'
        )

        #3. Create a shared network
        self.shared_network = Network.create(
            self.apiclient,
            self.services["network2"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            domainid=self.domain.id
            #physicalnetworkid=self.physical_network_3.id
        )
        self.cleanup.append(self.shared_network)

        # Create small service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["small"]
        )
        self.cleanup.append(self.service_offering)

        #4. Create virtual machine
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.shared_network.id
        )
        self.cleanup.append(self.virtual_machine)

        list_vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        vm = list_vms[0]
        # make sure that vm uses the same vlan of the network
        self.assertEqual(vm.nic[0].traffictype,
                         "Guest",
                         "Vm traffic type is not same")

        self.assertEqual(vm.nic[0].isolationuri,
                         "vlan://" + str(self.services["network2"]["vlan"]),
                         "Vm network vlan is not same")

        self.network_offering.update(
            self.apiclient,
            state='Disabled'
        )

        hosts = Host.list(
            self.apiclient,
            id=vm.hostid
        )
        if isinstance(hosts, list) and len(hosts) > 0:
            host = hosts[0]
        else:
            raise Exception("Cannot find the host where vm is running on")
        if host.hypervisor.lower() not in "kvm":
            return
        if self.kvmnetworklabel is None:
            return
        try:
            host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
            physical_nic_kvmnetworklabel= _execute_ssh_command(host.ipaddress, 22, host.user, host.passwd, "brctl show %s | grep cloudbr |awk '{print $4}'" % self.kvmnetworklabel)
            bridge_name = _execute_ssh_command(host.ipaddress, 22, host.user, host.passwd, "virsh domiflist %s |grep vnet |awk '{print $3}'" % vm.instancename)
        except Exception as e:
            return

        if bridge_name is not None and physical_nic_kvmnetworklabel is not None:
            if bridge_name[0].startswith("br%s-" %physical_nic_kvmnetworklabel[0]):
                self.logger.debug("vm is running on physical nic %s and bridge %s" % (physical_nic_kvmnetworklabel[0], bridge_name[0]))
            else:
                raise Exception("vm should be running on physical nic %s but on bridge" % (physical_nic_kvmnetworklabel[0], bridge_name[0]))

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_update_network_with_null_tags(self):
        """
        Update the tags to NULL for the existing physical network
        Since the zone has already "Management" network of traffic type Guest without any tags,
        we cant set the tags to null for the newly created networks
        :return:
        """
        try:
            # Set tags to null
            self.physical_network.update(
                self.apiclient,
                tags=""
            )

            # it should throw exception and should not come here
            raise Exception("Tags cannot be removed from network as there are more than 1 network without tags")
        except Exception as e:
            self.logger.info("Exception happened as expected while removing the tags")

        try:
            # Do the above steps for the second network
            self.physical_network_2.update(
                self.apiclient,
                tags=""
            )

            # it should throw exception and should not come here
            raise Exception("Tags cannot be removed from network as there are more than 1 network without tags")
        except Exception as e:
            self.logger.info("Exception happened as expected while removing the tags")

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_add_invalid_network_tags(self):
        """
        Try to add other traffic types like "Storage", "Public" and "Management"
        It should throw exception as there are physcial networks existing with this tag
        :return:
        """
        try:
            # Add traffic type storage
            self.physical_network.addTrafficType(
                self.apiclient,
                type="Storage"
            )

            # control should not come here
            raise Exception("Storage tag should be assigned as there is another network with the same tag")
        except Exception as e:
            self.logger.info("Exception happened as expected while adding traffic type")

        try:
            # Add traffic type Public
            self.physical_network.addTrafficType(
                self.apiclient,
                type="Public"
            )

            # control should not come here
            raise Exception("Public tag should be assigned as there is another network with the same tag")
        except Exception as e:
            self.logger.info("Exception happened as expected while adding traffic type")

        try:
            # Add traffic type Management
            self.physical_network.addTrafficType(
                self.apiclient,
                type="Management"
            )

            # control should not come here
            raise Exception("Management tag should be assigned as there is another network with the same tag")
        except Exception as e:
            self.logger.info("Exception happened as expected while adding traffic type")
