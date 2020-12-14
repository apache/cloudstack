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

""" Network migration test
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (
    Account,
    ServiceOffering,
    VirtualMachine,
    NetworkOffering,
    Network,
    VpcOffering,
    VPC
)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines)

from marvin.lib.utils import (get_hypervisor_type,
                              cleanup_resources)


# Import System Modules
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf


class Services:
    """Test network services
    """
    def __init__(self):
        self.services = {
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
        }


class TestNetworkMigration(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNetworkMigration, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.test_data = cls.testClient.getParsedTestDataConfig()
        cls.services = Services().services

        cls.hypervisorNotSupported = False
        hypervisor = get_hypervisor_type(cls.api_client)
        if hypervisor.lower() not in ["vmware", "kvm"]:
            cls.hypervisorNotSupported = True

        cls._cleanup = []
        if not cls.hypervisorNotSupported:
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(
                    cls.api_client,
                    cls.testClient.getZoneForTests())
            cls.template = get_template(
                    cls.api_client,
                    cls.zone.id,
                    cls.test_data["ostype"]
            )
            cls.services["virtual_machine"]["template"] = cls.template.id
            if cls.zone.localstorageenabled:
                cls.storagetype = 'local'
                cls.test_data["service_offerings"][
                    "tiny"]["storagetype"] = 'local'
            else:
                cls.storagetype = 'shared'
                cls.test_data["service_offerings"][
                    "tiny"]["storagetype"] = 'shared'

            cls.service_offering = ServiceOffering.create(
                    cls.api_client,
                    cls.test_data["service_offerings"]["tiny"]
            )

            # Create Network offering without userdata
            cls.network_offering_nouserdata = NetworkOffering.create(
                    cls.api_client,
                    cls.test_data["network_offering"]
            )
            # Enable Network offering
            cls.network_offering_nouserdata.update(cls.api_client,
                                                   state='Enabled')

            # Create Network Offering with all the serices
            cls.network_offering_all = NetworkOffering.create(
                    cls.api_client,
                    cls.test_data["isolated_network_offering"]
            )
            # Enable Network offering
            cls.network_offering_all.update(cls.api_client, state='Enabled')

            cls.native_vpc_network_offering = NetworkOffering.create(
                        cls.api_client,
                        cls.test_data["nw_offering_isolated_vpc"],
                        conservemode=False)
            cls.native_vpc_network_offering.update(cls.api_client,
                                                   state='Enabled')

            cls._cleanup = [
                cls.service_offering,
                cls.network_offering_nouserdata,
                cls.network_offering_all,
                cls.native_vpc_network_offering
            ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        if not self.hypervisorNotSupported:
            self.account = Account.create(
                    self.apiclient,
                    self.test_data["account"],
                    admin=True,
                    domainid=self.domain.id
            )
        self.cleanup = []
        return

    def tearDown(self):
        try:
            if not self.hypervisorNotSupported:
                self.account.delete(self.apiclient)
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def migrate_network(self, nw_off, network, resume=False):
        return network.migrate(self.api_client, nw_off.id, resume)

    def migrate_vpc(self, vpc, vpc_offering,
                    network_offering_map, resume=False):
        return vpc.migrate(self.api_client,
                           vpc_offering.id,
                           network_offering_map, resume)

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "smoke", "nativeisoonly"],
          required_hardware="false")
    def test_01_native_to_native_network_migration(self):
        """
        Verify Migration for an isolated network nativeOnly
        1. create native non-persistent isolated network
        2. migrate to other non-persistent isolated network
        3. migrate back to first native non-persistent network
        4. deploy VM in non-persistent isolated network
        5. migrate to native persistent isolated network
        6. migrate back to native non-persistent network
        """

        isolated_network = Network.create(
                self.apiclient,
                self.test_data["isolated_network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering_all.id,
                zoneid=self.zone.id
        )

        self.migrate_network(
                self.network_offering_nouserdata,
                isolated_network, resume=False)

        self.migrate_network(
                self.network_offering_all,
                isolated_network, resume=False)

        deployVmResponse = VirtualMachine.create(
                self.apiclient,
                services=self.test_data["virtual_machine_userdata"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[str(isolated_network.id)],
                templateid=self.template.id,
                zoneid=self.zone.id
        )
        vms = list_virtual_machines(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account"
                                   " %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id),
                     "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")

        self.migrate_network(
                self.network_offering_nouserdata,
                isolated_network, resume=False)

        self.migrate_network(
                self.network_offering_all,
                isolated_network, resume=False)

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "smoke", "nativevpconly"],
          required_hardware="false")
    def test_02_native_to_native_vpc_migration(self):
        """
        Verify Migration for a vpc network nativeOnly
        1. create native vpc with 2 tier networks
        2. migrate to native vpc, check VR state
        3. deploy VM in vpc tier network
        4. acquire ip and enable staticnat
        5. migrate to native vpc network
        """

        self.debug("Creating Native VSP VPC offering with Static NAT service "
                   "provider as VPCVR...")
        native_vpc_off = VpcOffering.create(
                self.apiclient,
                self.test_data["vpc_offering_reduced"])

        self.debug("Enabling the VPC offering created")
        native_vpc_off.update(self.apiclient, state='Enabled')

        self.debug("Creating a VPC with Static NAT service provider as "
                   "VpcVirtualRouter")
        self.services["vpc"]["cidr"] = '10.1.1.1/16'
        vpc = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=native_vpc_off.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
        )
        self.debug("Creating native VPC Network Tier offering "
                   "with Static NAT service provider as VPCVR")
        native_tiernet_off = \
            NetworkOffering.create(self.apiclient,
                                   self.test_data
                                   ["nw_offering_reduced_vpc"],
                                   conservemode=False)
        native_tiernet_off.update(self.apiclient, state='Enabled')

        self.debug("Creating a VPC tier network with Static NAT service")
        vpc_tier = Network.create(self.apiclient,
                                  self.services["network"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  networkofferingid=native_tiernet_off.id,
                                  zoneid=self.zone.id,
                                  gateway='10.1.1.1',
                                  vpcid=vpc.id if vpc else self.vpc.id
                                  )
        self.debug("Created network with ID: %s" % vpc_tier.id)

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.native_vpc_network_offering.id}]

        self.migrate_vpc(vpc, native_vpc_off,
                         network_offering_map, resume=False)

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": native_tiernet_off.id}]

        self.migrate_vpc(vpc, native_vpc_off,
                         network_offering_map, resume=False)

        self.debug('Creating VM in network=%s' % native_tiernet_off.name)
        vm = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[str(vpc_tier.id)],
                templateid=self.template.id,
                zoneid=self.zone.id
        )
        self.debug('Created VM=%s in network=%s' %
                   (vm.id, native_tiernet_off.name))

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": self.native_vpc_network_offering.id}]

        self.migrate_vpc(vpc, native_vpc_off,
                         network_offering_map, resume=False)

        network_offering_map = \
            [{"networkid": vpc_tier.id,
              "networkofferingid": native_tiernet_off.id}]

        self.migrate_vpc(vpc, native_vpc_off,
                         network_offering_map, resume=False)
