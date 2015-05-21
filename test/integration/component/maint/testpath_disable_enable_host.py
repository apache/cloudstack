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
""" Test cases for Disable enable Host Test Path
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Account,
                             Host,
                             Cluster,
                             VirtualMachine,
                             ServiceOffering,
                             Snapshot,
                             DiskOffering,
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_pod,
                               list_volumes,
                               )

from marvin.cloudstackAPI import (updateHost,
                                  reconnectHost
                                  )
import time


class TestDisableEnableZone(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDisableEnableZone, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(
            cls.apiclient,
            zone_id=cls.zone.id)

        hostList = Host.list(cls.apiclient, zoneid=cls.zone.id, type="routing")
        clusterList = Cluster.list(cls.apiclient, id=hostList[0].clusterid)
        cls.host = Host(hostList[0].__dict__)
        cls.cluster = Cluster(clusterList[0].__dict__)

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        try:
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"],
            )
            cls._cleanup.append(cls.service_offering)

            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.testdata["disk_offering"],
            )
            cls._cleanup.append(cls.disk_offering)

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            pass
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic"], required_hardware="true")
    def test_01_disable_enable_host(self):
        """disable enable host
            1. Disable host and verify following things:
                For admin user:
                    1. Should be create to start/stop exsiting vms
                    2. Should be create to deploy new vm,
                    snapshot on the same host
                For Non-admin user:
                    1. Should be create to start/stop exsiting vms
                    2. Should not be create to deploy new vm,
                    snapshot on the same host
            2. Enable the above disabled host and verify that:
                -All users should be create to deploy new vm,
                snapshot on the same host
            3. Try to reconnect the host :
                -Host should get reconnected successfully
        """
        # Step 1
        vm_root = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )
        hostid = vm_root.hostid

        vm_user = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        cmd = updateHost.updateHostCmd()
        cmd.id = hostid
        cmd.resourcestate = "Disabled"
        cmd.allocationstate = "Disable"
        self.apiclient.updateHost(cmd)
        hostList = Host.list(self.apiclient, id=hostid)

        self.assertEqual(hostList[0].resourcestate,
                         "Disabled",
                         "Check if the host is in disabled state"
                         )
        # Verify the exsisting vms should be running
        self.assertEqual(vm_user.state,
                         "Running",
                         "Verify that the user vm is running")

        self.assertEqual(vm_root.state,
                         "Running",
                         "Verify that the root vm is running")

        vm_root.stop(self.apiclient)
        vm_user.stop(self.apiclient)
        root_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_root.name)[0][0]
        user_state = self.dbclient.execute(
            "select state from vm_instance where name='%s'" %
            vm_user.name)[0][0]

        self.assertEqual(root_state,
                         "Stopped",
                         "verify that vm should stop")

        self.assertEqual(user_state,
                         "Stopped",
                         "verify that vm should stop")

        VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            hostid=hostid)

        root_volume = list_volumes(
            self.apiclient,
            virtualmachineid=vm_root.id,
            type='ROOT',
            listall=True
        )

        snap = Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should be \
                                    able to create snapshot")

        # non-admin user should fail to create vm, snap, temp etc

        with self.assertRaises(Exception):
            snap = Snapshot.create(
                self.userapiclient,
                root_volume[0].id)

        # Step 2
        cmd.resourcestate = "Enabled"
        cmd.allocationstate = "Enable"
        self.apiclient.updateHost(cmd)

        hostList = Host.list(self.apiclient, id=hostid)

        self.assertEqual(hostList[0].resourcestate,
                         "Enabled",
                         "Check if the host is in enabled state"
                         )
        # After enabling the zone all users should be able to add new VM,
        # volume, templatee and iso

        root_vm_new = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            hostid=hostid)

        self.assertNotEqual(root_vm_new,
                            None,
                            "Verify that admin should create new VM")

        snap = Snapshot.create(
            self.apiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should be \
                                    able to create snapshot")

        # Non root user
        user_vm_new = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
        )

        self.assertNotEqual(user_vm_new,
                            None,
                            "Verify that admin should create new VM")

        snap = Snapshot.create(
            self.userapiclient,
            root_volume[0].id)

        self.assertNotEqual(snap,
                            None,
                            "Verify that admin should be \
                                    able to create snapshot")

        # Step 4
        # reconnect the host
        cmd = reconnectHost.reconnectHostCmd()
        cmd.id = hostid
        self.apiclient.reconnectHost(cmd)
        # Host takes some time to come back to Up state so included sleep
        time.sleep(90)
        hostList = Host.list(self.apiclient, id=hostid)

        self.assertEqual(hostList[0].state,
                         "Up",
                         "Check if the host get reconnected successfully"
                         )

        return
