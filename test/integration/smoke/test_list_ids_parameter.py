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
""" Tests for API listing methods using 'ids' parameter
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             Volume,
                             DiskOffering,
                             Template,
                             ServiceOffering,
                             Snapshot,
                             VmSnapshot,
                             VirtualMachine)
from marvin.lib.common import (get_domain,
                                get_zone, get_test_template)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True
class TestListIdsParams(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListIdsParams, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )

        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.service_offering = ServiceOffering.create(
                                            cls.apiclient,
                                            cls.services["service_offerings"]["tiny"]
                                            )

        template = get_test_template(
                            cls.apiclient,
                            cls.zone.id,
                            cls.hypervisor
                            )
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["template"]["ostypeid"] = template.ostypeid
        cls.services["template_2"]["ostypeid"] = template.ostypeid
        cls.services["ostypeid"] = template.ostypeid
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["mode"] = cls.zone.networktype

        #Create 3 VMs
        cls.virtual_machine_1 = VirtualMachine.create(
                                cls.apiclient,
                                cls.services["virtual_machine"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls.virtual_machine_2 = VirtualMachine.create(
                                cls.apiclient,
                                cls.services["virtual_machine"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls.virtual_machine_3 = VirtualMachine.create(
                                cls.apiclient,
                                cls.services["virtual_machine"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )

#        Take 3 VM1 Snapshots
#        PLEASE UNCOMMENT ONCE VM SNAPSHOT DELAY BUG AFTER VM CREATION IS FIXED
#        cls.vmsnapshot_1 = VmSnapshot.create(
#                                cls.apiclient,
#                                cls.virtual_machine_1.id
#                            )
#        cls.vmsnapshot_2 = VmSnapshot.create(
#                                cls.apiclient,
#                                cls.virtual_machine_1.id
#                            )
#        cls.vmsnapshot_3 = VmSnapshot.create(
#                                cls.apiclient,
#                                cls.virtual_machine_1.id
#                            )

        #Stop VMs
        cls.virtual_machine_1.stop(cls.apiclient)
        cls.virtual_machine_2.stop(cls.apiclient)
        cls.virtual_machine_3.stop(cls.apiclient)

        #Get ROOT volumes of 3 VMs
        vm1RootVolumeResponse = Volume.list(
                                            cls.apiclient,
                                            virtualmachineid=cls.virtual_machine_1.id,
                                            type='ROOT',
                                            listall=True
                                            )
        vm2RootVolumeResponse = Volume.list(
                                            cls.apiclient,
                                            virtualmachineid=cls.virtual_machine_2.id,
                                            type='ROOT',
                                            listall=True
                                            )
        vm3RootVolumeResponse = Volume.list(
                                            cls.apiclient,
                                            virtualmachineid=cls.virtual_machine_3.id,
                                            type='ROOT',
                                            listall=True
                                            )
        cls.vm1_root_volume = vm1RootVolumeResponse[0]
        cls.vm2_root_volume = vm2RootVolumeResponse[0]
        cls.vm3_root_volume = vm3RootVolumeResponse[0]

        #Take 3 snapshots of VM2's ROOT volume
        cls.snapshot_1 = Snapshot.create(
                                         cls.apiclient,
                                         cls.vm2_root_volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )
        cls.snapshot_2 = Snapshot.create(
                                         cls.apiclient,
                                         cls.vm2_root_volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )
        cls.snapshot_3 = Snapshot.create(
                                         cls.apiclient,
                                         cls.vm2_root_volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )

        #Create 3 templates
        cls.template_1 = Template.create(
                                         cls.apiclient,
                                         cls.services["template"],
                                         cls.vm3_root_volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )
        cls.template_2 = Template.create(
                                         cls.apiclient,
                                         cls.services["template_2"],
                                         cls.vm3_root_volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )
        cls.template_3 = Template.create(
                                         cls.apiclient,
                                         cls.services["template_2"],
                                         cls.vm3_root_volume.id,
                                         account=cls.account.name,
                                         domainid=cls.account.domainid
                                         )

        cls._cleanup = [
                        cls.snapshot_1,
                        cls.snapshot_2,
                        cls.snapshot_3,
                        cls.account,
                        cls.disk_offering,
                        cls.service_offering
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestListIdsParams, cls).getClsTestClient().getApiClient()
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_volumes(self):
        """Test listing Volumes using 'ids' parameter
        """
        list_volume_response = Volume.list(
                                           self.apiclient,
                                           ids=[self.vm1_root_volume.id, self.vm2_root_volume.id, self.vm3_root_volume.id],
                                           type='ROOT',
                                           listAll=True
                                           )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "List Volume response was not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            3,
            "ListVolumes response expected 3 Volumes, received %s" % len(list_volume_response)
        )

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_templates(self):
        """Test listing Templates using 'ids' parameter
        """
        list_template_response = Template.list(
                                    self.apiclient,
                                    templatefilter='all',
                                    ids=[self.template_1.id, self.template_2.id, self.template_3.id],
                                    account=self.account.name,
                                    domainid=self.account.domainid,
                                    listAll=True
                                    )
        self.assertEqual(
            isinstance(list_template_response, list),
            True,
            "ListTemplates response was not a valid list"
        )
        self.assertEqual(
            len(list_template_response),
            3,
            "ListTemplates response expected 3 Templates, received %s" % len(list_template_response)
        )

    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_snapshots(self):
        """Test listing Snapshots using 'ids' parameter
        """
        list_snapshot_response = Snapshot.list(
                                    self.apiclient,
                                    ids=[self.snapshot_1.id, self.snapshot_2.id, self.snapshot_3.id],
                                    listAll=True
                                    )
        self.assertEqual(
            isinstance(list_snapshot_response, list),
            True,
            "ListSnapshots response was not a valid list"
        )
        self.assertEqual(
            len(list_snapshot_response),
            3,
            "ListSnapshots response expected 3 Snapshots, received %s" % len(list_snapshot_response)
        )

#    PLEASE UNCOMMENT ONCE VM SNAPSHOT DELAY BUG AFTER VM CREATION IS FIXED
#    @attr(tags = ["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
#    def test_04_list_vm_snapshots(self):
#        """Test listing VMSnapshots using 'vmsnapshotids' parameter
#        """
#        list_vm_snapshot_response = VmSnapshot.list(
#                                        self.apiclient,
#                                        vmsnapshotids=[self.vmsnapshot_1.id, self.vmsnapshot_2.id, self.vmsnapshot_3.id],
#                                        listall=True
#                                        )
#        self.assertEqual(
#            isinstance(list_vm_snapshot_response, list),
#            True,
#            "ListVMSnapshots response was not a valid list"
#        )
#        self.assertEqual(
#            len(list_vm_snapshot_response),
#            3,
#            "ListVMSnapshots response expected 3 VMSnapshots, received %s" % len(list_vm_snapshot_response)
#        )
