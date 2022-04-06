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

"""  tests for find Host for migration both suitable and not suitable
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (migrateVirtualMachine,
                                  prepareHostForMaintenance,
                                  cancelHostMaintenance,
                                  findHostsForMigration)
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Cluster,
                             Host,
                             Configurations)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_hosts,
                               list_virtual_machines,
                               list_service_offering,
                               findSuitableHostForMigration)
import time


class Services:

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "HA",
                "lastname": "HA",
                "username": "HA",
                # Random characters are appended for unique
                # username
                "password": "password",
                },
            "service_offering_with_tag": {
                "name": "Tiny Instance With hosttag",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100, # in MHz
                "memory": 128, # In MBs
                "hosttags": "PREMIUM",
            },
            "virtual_machine": {
                "displayname": "VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
                },
            "ostype": 'CentOS 5.3 (64-bit)',
            "timeout": 100,
            }


class TestHostsForMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHostsForMigration, cls).getClsTestClient()
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


        clusterWithSufficientHosts = None
        clusters = Cluster.list(cls.api_client, zoneid=cls.zone.id)
        for cluster in clusters:
            cls.hosts = Host.list(cls.api_client, clusterid=cluster.id, type="Routing")
            if len(cls.hosts) >= 2:
                clusterWithSufficientHosts = cluster
                break

        if clusterWithSufficientHosts is None:
            raise unittest.SkipTest("No Cluster with 2 hosts found")


        Host.update(cls.api_client, id=cls.hosts[1].id, hosttags="PREMIUM")

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering_with_tag = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering_with_tag"]
        )

        cls._cleanup = [
            cls.service_offering_with_tag,
            ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Host.update(cls.api_client, id=cls.hosts[1].id, hosttags="")
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
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(required_hardware="false")
    @attr(tags=["advanced", "basic", "simulator"])
    def test_01_find_hosts_for_migration(self):
        """ Test find suitable and not-suitable list of hosts for migration """

        # Steps,
        #1. Create a Compute service offering with the tag .
        #2. Create a Guest VM with the compute service offering created above.
        #3. find hosts to migrate the vm crated above

        # Validations,
        #1. Ensure that the offering is created with the tag
        #The listServiceOffering API should list show tag
        #2. Select the newly created VM and ensure that the Compute offering field value lists the compute service offering that was selected.
        #3. findHostsForMigration cmd should list both suitable and not-suitable hosts

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_with_tag.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "listServiceOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_service_response),
            0,
            "listServiceOfferings returned empty list."
        )
        self.assertEqual(
            list_service_response[0].hosttags,
            "PREMIUM",
            "The service offering having tag"
        )

        #create virtual machine with the service offering with Ha enabled
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_with_tag.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )
        self.debug("Deployed VM on host: %s" % vms[0].hostid)

        #verify that the virtual machine created on the host with tag
        list_hosts_response = list_hosts(
            self.apiclient,
            id=virtual_machine.hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "listHosts returned invalid object in response."
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "listHosts returned empty list."
        )

        host = list_hosts_response[0]

        self.assertEqual(
            host.hosttags,
            "PREMIUM",
            "VM is created on a host having appropriate tag. %s" % host.uuid
        )

        try:
            list_hosts_response = Host.listForMigration(self.apiclient, virtualmachineid=virtual_machine.id,
                                      )
        except Exception as e:
            raise Exception("Exception while getting hosts list suitable for migration: %s" % e)


        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "listHosts returned invalid object in response."
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "listHosts returned empty response."
        )
        suitableHost = set()
        notSuitableHost = set()

        for host in list_hosts_response:
            if host.suitableformigration:
                suitableHost.add(host)
            else:
                notSuitableHost.add(host)

        self.assertTrue(notSuitableHost is not None, "notsuitablehost should not be None")
        self.debug("Suitable Hosts: %s" % suitableHost)
        self.debug("Not suitable Hosts: %s" % notSuitableHost)
