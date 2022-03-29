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
""" Test cases to Check Snapshots size in database
"""


from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              )
from marvin.lib.base import (Account,
                             Network,
                             NetworkOffering,
                             ServiceOffering,
                             VirtualMachine,
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_networks)


def ipv4_cidr_to_netmask(bits):
    """ Convert CIDR bits to netmask """
    netmask = ''
    for i in range(4):
        if i:
            netmask += '.'
        if bits >= 8:
            netmask += '%d' % (2 ** 8 - 1)
            bits -= 8
        else:
            netmask += '%d' % (256 - 2 ** (8 - bits))
            bits = 0
    return netmask


class TestCheckNetmask(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCheckNetmask, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        cls.skiptest = False

        if cls.hypervisor.lower() not in ["xenserver"]:
            cls.skiptest = True
            return

        try:

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )

            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )

            cls.testdata["shared_network_offering"]["specifyVlan"] = 'True'
            cls.testdata["shared_network_offering"]["specifyIpRanges"] = 'True'

            cls.shared_network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.testdata["shared_network_offering"]
            )

            NetworkOffering.update(
                cls.shared_network_offering,
                cls.apiclient,
                id=cls.shared_network_offering.id,
                state="enabled"
            )

            cls.network = Network.create(
                cls.apiclient,
                cls.testdata["network2"],
                networkofferingid=cls.shared_network_offering.id,
                zoneid=cls.zone.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid
            )

            cls.vm = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["small"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                networkids=cls.network.id,
            )

            cls._cleanup.extend([cls.account,
                                 cls.service_offering,
                                 cls.shared_network_offering])

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        if self.skiptest:
            self.skipTest(
                "Test not to be run on %s" %
                self.hypervisor)
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_netmask_value_check(self):
        """ Check Netmask value in database
            1. Check if netmask attribute in nics table
                stores correct value.
        """

        # Step 1
        # Get the netmask from ipv4 address of the VM
        qryresult_netmask = self.dbclient.execute(
            " select id, uuid, netmask\
                    from nics where ip4_address='%s';" %
            self.vm.nic[0].ipaddress)

        self.assertNotEqual(
            len(qryresult_netmask),
            0,
            "Check if netmask attribute in nics table \
            stores correct value")

        # Step 2
        netmask_id = qryresult_netmask[0][2]
        netlist = list_networks(self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid)

        self.assertNotEqual(len(netlist), 0,
                            "Check if list networks returned an empty list.")

        cidr = netlist[0].cidr.split("/")[1]
        # Get netmask from CIDR
        netmask = ipv4_cidr_to_netmask(int(cidr))

        # Validate the netmask
        self.assertEqual(netmask_id,
                         netmask,
                         "Check if the netmask is from guest CIDR")
        return
