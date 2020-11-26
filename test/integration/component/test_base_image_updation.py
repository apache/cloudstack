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

""" Component tests for Base Image Updation functionality

    Feature Specifications: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Base+Image+Update+facility

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/Base+Image+Updation+facility+TestPlan

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-2243
"""

#Import Local Modules
from marvin.codes import (PASS,
                          RECURRING)
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest

from marvin.lib.base import (ServiceOffering,
                                         Account,
                                         VirtualMachine,
                                         Volume,
                                         Host,
                                         Snapshot,
                                         SnapshotPolicy,
                                         Template
                                         )

from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           list_templates
                                           )

from marvin.lib.utils import (validateList,
                                          cleanup_resources)

import time

class Services:
    """Test Base Image Updation
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering_with_reset": {
                "name": "Tiny Instance With Reset",
                "displaytext": "Tiny Instance With Reset",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "isvolatile": True,
            },
            "service_offering_without_reset": {
                "name": "Tiny Instance Without Reset",
                "displaytext": "Tiny Instance Without Reset",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "isvolatile": False,
            },
            "recurring_snapshot": {
                "intervaltype": 'HOURLY',
                # Frequency of snapshots
                "maxsnaps": 2,  # Should be min 2
                "schedule": 1,
                "timezone": 'America/New_York',
                # Timezone Formats - http://cloudstack.apache.org/docs/en-US/Apache_CloudStack/4.0.0-incubating/html-single/API_Developers_Guide/#time-zones
            },
            "templates": {
                # Configs for different Template formats
                # For Eg. raw image, zip etc
                "XenServer": {
                       "displaytext": "Public Template - Xen",
                        "name": "Public template - Xen",
                        "ostype": "CentOS 5.3 (64-bit)",
                        "url": "http://download.cloudstack.org/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                        "hypervisor": "xenserver",
                        "format": "VHD",
                        "isfeatured": True,
                        "ispublic": True,
                        "isextractable": True,
                },
                "KVM": {
                        "displaytext": "Public Template - KVM",
                        "name": "Public template -KVM",
                        "ostype": "CentOS 5.3 (64-bit)",
                        "url": "http://download.cloudstack.org/releases/2.0.0/UbuntuServer-10-04-64bit.qcow2.bz2",
                        "hypervisor": "kvm",
                        "format": "qcow2",
                        "isfeatured": True,
                        "ispublic": True,
                        "isextractable": True,

                },
                "VMware": {
                        "displaytext": "Public Template - VMware",
                        "name": "Public template -VMware",
                        "ostype": "CentOS 5.3 (64-bit)",
                        "url": "http://download.cloudstack.org/releases/2.2.0/CentOS5.3-x86_64.ova",
                        "hypervisor": "vmware",
                        "format": "ova",
                        "isfeatured": True,
                        "ispublic": True,
                        "isextractable": True,

                }
            },
            "template": {
                    "displaytext": "Cent OS Template 2",
                    "name": "Cent OS Template 2",
                    "ostype": "CentOS 5.3 (64-bit)",
                    "templatefilter": "self",
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": "XenServer",
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP",
                "userdata": "This is sample data",
            },
            "ostype": "CentOS 5.3 (64-bit)",
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "retriesCount": 10,
            "mode": "advanced"
        }


class TestBaseImageUpdate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestBaseImageUpdate, cls).getClsTestClient()
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering_with_reset = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering_with_reset"]
                                            )

        cls.service_offering_without_reset = ServiceOffering.create(
                                                cls.api_client,
                                                cls.services["service_offering_without_reset"]
                                                )

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     admin=True,
                                     domainid=cls.domain.id
                                     )

        cls.vm_with_reset = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering_with_reset.id,
                                  )
        cls.vm_with_reset_root_disk_id = cls.get_root_device_uuid_for_vm(cls.vm_with_reset.id,
                                                cls.vm_with_reset.rootdeviceid)


        cls.vm_without_reset = VirtualMachine.create(
                                  cls.api_client,
                                  cls.services["virtual_machine"],
                                  accountid=cls.account.name,
                                  domainid=cls.account.domainid,
                                  serviceofferingid=cls.service_offering_without_reset.id,
                                  )
        cls.vm_without_reset_root_disk_id = cls.get_root_device_uuid_for_vm(cls.vm_without_reset.id,
                                                cls.vm_without_reset.rootdeviceid)

        cls._cleanup = [
                        cls.account,
                        cls.service_offering_with_reset,
                        cls.service_offering_without_reset,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def get_root_device_uuid_for_vm(cls, vm_id, root_device_id):
        volumes = Volume.list(cls.api_client, virtualmachineid=vm_id, listall=True)
        return volumes[root_device_id].id

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def verify_template_listing(self, template):

        retriesCount = int(self.services["retriesCount"])

        template_list_validation_result = None

        while True:
            list_template_response = list_templates(
                                self.apiclient,
                                templatefilter=\
                                self.services["template"]["templatefilter"],
                                id=template.id,
                                zoneid=self.zone.id,
                                account=self.account.name,
                                domainid=self.account.domainid
                                )

            template_list_validation_result = validateList(list_template_response)

            if template_list_validation_result[0] == PASS:
                break

            elif retriesCount == 0:
                self.fail("Failed to get the template list")

	        # Sleep for 5 seconds and again continue the loop if retriesCount has not reached zero
            time.sleep(5)

	        #Reduce the retriesCount until it becomes zero, when it reaches zero, exception is raised
            retriesCount = retriesCount - 1

        template_response = template_list_validation_result[1]

        self.assertEqual(
                        template_response.isready,
                        True,
                        "Check isready of newly created template Expected :True Got:%s" %template_response.isready
                    )

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_deploy_instance_with_is_volatile_offering(self):
        """ Test deploy an instance with service offerings with IsVolatile set.
        """

        # Validate the following
        # 1. Service offerings were created successfully
        # 2. Vms were successfully deployed with the service offerings.

        self.debug("Checking if deployed VMs are in running state...")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  account=self.account.name,
                                  domainid=self.account.domainid,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "VM list validation failed due to %s" %
			 vm_list_validation_result[2])

        for vm in vms:
            self.debug("VM name: %s, VM state: %s" % (vm.name, vm.state))
            self.debug("%s" %vm)
            self.assertEqual(
                             vm.state,
                             "Running",
                             "Vm state should be running for each VM deployed"
                             )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_reboot_instance_with_is_volatile_offering(self):
        """ Test rebooting instances created with isVolatile service offerings
        """

        # Validate the following
        # 1. Reboot the virtual machines.
        # 2. Validate the following
        #     a. VM with created with isVolatile=True should have new Root disk but same IP
        #     b. VM with created with isVolatile=False should have same Root disk and IP as before reboot

        self.debug("Rebooting the virtual machines in account: %s" %
                                                self.account.name)
        try:
            self.vm_with_reset.reboot(self.apiclient)
            self.vm_without_reset.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual machines, %s" % e)

        # Check if the the root disk was destroyed and recreated for isVolatile=True
        self.debug("Checking root disk of VM with isVolatile=True")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_with_reset.id,
                                  listall=True
                                  )

        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "VM list validation failed due to %s" %
			 vm_list_validation_result[2])

        vm_with_reset = vm_list_validation_result[1]
        vm_with_reset_root_disk_id = self.get_root_device_uuid_for_vm(vm_with_reset.id, vm_with_reset.rootdeviceid)

        self.assertNotEqual(self.vm_with_reset_root_disk_id,
                            vm_with_reset_root_disk_id,
                            "VM created with IsVolatile=True has same rootdeviceid : %s after reboot" %vm_with_reset_root_disk_id
                        )
        # Make sure it has the same IP after reboot
        self.assertEqual(self.vm_with_reset.nic[0].ipaddress,
                            vm_with_reset.nic[0].ipaddress,
                            "VM created with IsVolatile=True doesn't have same ip after reboot. Got : %s Expected : %s"
                            %(vm_with_reset.nic[0].ipaddress, self.vm_with_reset.nic[0].ipaddress)
                        )

        # Check if the the root disk was not destroyed for isVolatile=False
        self.debug("Checking root disk of VM with isVolatile=False")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_without_reset.id,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "list validation failed due to %s" %
			 vm_list_validation_result[2])

        vm_without_reset = vm_list_validation_result[1]

        vm_without_reset_root_disk_id = self.get_root_device_uuid_for_vm(vm_without_reset.id,
                                            vm_without_reset.rootdeviceid)

        self.assertEqual(self.vm_without_reset_root_disk_id,
                        vm_without_reset_root_disk_id,
                        "VM created with IsVolatile=False has different rootdeviceid after reboot Got: %s Expected : %s"
                        %(vm_without_reset_root_disk_id, self.vm_without_reset_root_disk_id)
                        )

        # Make sure it has the same IP after reboot
        self.assertEqual(self.vm_without_reset.nic[0].ipaddress,
                        vm_without_reset.nic[0].ipaddress,
                        "VM created with IsVolatile=True doesn't have same ip after reboot. Got : %s Expected : %s"
                        %(vm_without_reset.nic[0].ipaddress, self.vm_without_reset.nic[0].ipaddress)
                        )

        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_03_restore_vm_with_new_template(self):
        """ Test restoring a vm with different template than the one it was created with
        """

        hosts = Host.list(
                          self.apiclient,
                          type="Routing",
                          listall=True
                          )

        host_list_validation_result = validateList(hosts)

        self.assertEqual(host_list_validation_result[0], PASS, "host list validation failed due to %s" %
                         host_list_validation_result[2])

        hypervisor = host_list_validation_result[1].hypervisor

        for k, v in list(self.services["templates"].items()):
            if k.lower() == hypervisor.lower():
                # Register new template
                template = Template.register(
                                        self.apiclient,
                                        v,
                                        zoneid=self.zone.id,
                                        account=self.account.name,
                                        domainid=self.account.domainid,
                                        hypervisor=self.hypervisor
                                        )
                self.debug(
                    "Registered a template of format: %s with ID: %s" % (
                                                                v["format"],
                                                                template.id
                                                                ))
                self.debug(
                    "Downloading template with ID: %s" % (
                                                                template.id
                                                                ))
                template.download(self.apiclient)
                self._cleanup.append(template)

                # Wait for template status to be changed across
                time.sleep(self.services["sleep"])

                self.verify_template_listing(template)

                # Restore a vm with the new template.
                self.vm_with_reset.restore(self.apiclient, templateid=template.id)
                self.vm_without_reset.restore(self.apiclient, templateid=template.id)

                # Make sure the VMs now have the new template ID
                # Make sure the Ip address of the VMs haven't changed
                self.debug("Checking template id of VM with isVolatile=True")
                vms = VirtualMachine.list(
                                          self.apiclient,
                                          id=self.vm_with_reset.id,
                                          listall=True
                                          )

                vm_list_validation_result = validateList(vms)

                self.assertEqual(vm_list_validation_result[0], PASS, "VM list validation failed due to %s" %
			                     vm_list_validation_result[2])

                vm_with_reset = vm_list_validation_result[1]

                self.assertNotEqual(self.vm_with_reset.templateid,
                                    vm_with_reset.templateid,
                                    "VM created with IsVolatile=True has same templateid : %s after restore" %vm_with_reset.templateid
                                )

                self.assertNotEqual(self.vm_with_reset.templateid,
                                    template.id,
                                    "VM created with IsVolatile=True has wrong templateid after restore Got:%s Expected: %s"
                                    %(self.vm_with_reset.templateid, template.id)
                                )
                # Make sure it has the same IP after reboot
                self.assertEqual(self.vm_with_reset.nic[0].ipaddress,
                                    vm_with_reset.nic[0].ipaddress,
                                    "VM created with IsVolatile=True doesn't have same ip after restore. Got : %s Expected : %s"
                                    %(vm_with_reset.nic[0].ipaddress, self.vm_with_reset.nic[0].ipaddress)
                                )

                # Check if the the root disk was not destroyed for isVolatile=False
                self.debug("Checking template id of VM with isVolatile=False")
                vms = VirtualMachine.list(
                                          self.apiclient,
                                          id=self.vm_without_reset.id,
                                          listall=True
                                          )

                vm_list_validation_result = validateList(vms)

                self.assertEqual(vm_list_validation_result[0], PASS, "VM list validation failed due to %s" %
			                     vm_list_validation_result[2])

                vm_without_reset = vm_list_validation_result[1]

                self.assertNotEqual(self.vm_without_reset.templateid,
                                    vm_without_reset.templateid,
                                    "VM created with IsVolatile=False has same templateid : %s after restore" %vm_with_reset.templateid
                                )

                self.assertNotEqual(self.vm_without_reset.templateid,
                                    template.id,
                                    "VM created with IsVolatile=False has wrong templateid after restore Got:%s Expected: %s"
                                    %(self.vm_without_reset.templateid, template.id)
                                )
                # Make sure it has the same IP after reboot
                self.assertEqual(self.vm_without_reset.nic[0].ipaddress,
                                    vm_without_reset.nic[0].ipaddress,
                                    "VM created with IsVolatile=False doesn't have same ip after restore. Got : %s Expected : %s"
                                    %(vm_without_reset.nic[0].ipaddress, self.vm_without_reset.nic[0].ipaddress)
                                )
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_04_reoccuring_snapshot_rules(self):
        """
        1) Create a VM using the Service offering IsVolatile enabled
        2) Apply a recurring snapshot rule on the  Volume.
        3) After a couple of snapshots are taken reboot the VM.

        Verify the following conditions
        1) New root disk should be formed
        2) The recurring snapshot rule should be deleted
        """
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("snapshot creation is not supported in LXC")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_with_reset.id,
                                  listall=True
                                  )

        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
			 vm_list_validation_result[2])

        vm_with_reset = vm_list_validation_result[1]
        vm_with_reset_root_disk_id = self.get_root_device_uuid_for_vm(
                                            vm_with_reset.id,
                                            vm_with_reset.rootdeviceid
                                        )

        self.debug("Creating recurring snapshot policy for root disk on vm created with IsVolatile=True")
        self.debug("Snapshot Policy - Type : %s Scheduled Hours : %s" %(
            self.services["recurring_snapshot"]["intervaltype"],
            self.services["recurring_snapshot"]["schedule"]))

        recurring_snapshot = SnapshotPolicy.create(
                                           self.apiclient,
                                           vm_with_reset_root_disk_id,
                                           self.services["recurring_snapshot"]
                                        )

        #ListSnapshotPolicy should return newly created policy
        list_snapshots_policy = SnapshotPolicy.list(
                                                     self.apiclient,
                                                     id=recurring_snapshot.id,
                                                     volumeid=vm_with_reset_root_disk_id
                                                     )

        snapshot_list_validation_result = validateList(list_snapshots_policy)

        self.assertEqual(snapshot_list_validation_result[0], PASS, "snapshot list validation failed due to %s" %
			 snapshot_list_validation_result[2])

        snapshots_policy = snapshot_list_validation_result[1]

        self.assertEqual(
                        snapshots_policy.id,
                        recurring_snapshot.id,
                        "Check recurring snapshot id in list resources call"
                        )
        self.assertEqual(
                        snapshots_policy.maxsnaps,
                        self.services["recurring_snapshot"]["maxsnaps"],
                        "Check interval type in list resources call"
                        )
        sleep_seconds = (self.services["recurring_snapshot"]["schedule"]) * 3600 + 600
        sleep_minutes = sleep_seconds/60
        self.debug("Sleeping for %s minutes till the volume is snapshoted" %sleep_minutes)
        time.sleep(sleep_seconds)

        retriesCount = self.services["retriesCount"]
        while True:
            snapshots = Snapshot.list(
                        self.apiclient,
                        volumeid=vm_with_reset_root_disk_id,
                        intervaltype=\
                        self.services["recurring_snapshot"]["intervaltype"],
                        snapshottype=RECURRING,
                        listall=True
                        )

            snapshot_list_validation_result = validateList(snapshots)

            if snapshot_list_validation_result[0] == PASS:
                break

            elif retriesCount == 0:
                self.fail("Failed to get snapshots list")

            time.sleep(60)
            retriesCount = retriesCount - 1

        # rebooting the vm with isVolatile = True
        try:
            self.vm_with_reset.reboot(self.apiclient)
        except Exception as e:
            self.fail("Failed to reboot the virtual machine. Error: %s" % e)

        # Check if the the root disk was destroyed and recreated for isVolatile=True
        self.debug("Checking whether root disk of VM with isVolatile=True was destroyed")
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.vm_with_reset.id,
                                  listall=True
                                  )

        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "list validation failed due to %s" %
			 vm_list_validation_result[2])

        vm_with_reset_after_reboot = vm_list_validation_result[1]

        vm_with_reset_root_disk_id_after_reboot = self.get_root_device_uuid_for_vm(
                                                        vm_with_reset_after_reboot.id,
                                                        vm_with_reset_after_reboot.rootdeviceid
                                                    )

        self.assertNotEqual(vm_with_reset_root_disk_id,
                            vm_with_reset_root_disk_id_after_reboot,
                            "VM created with IsVolatile=True has same rootdeviceid : %s after reboot" %vm_with_reset_root_disk_id_after_reboot
                        )
        # Make sure it has the same IP after reboot
        self.assertEqual(vm_with_reset.nic[0].ipaddress,
                            vm_with_reset_after_reboot.nic[0].ipaddress,
                            "VM created with IsVolatile=True doesn't have same ip after reboot. Got : %s Expected : %s"
                            %(vm_with_reset_after_reboot.nic[0].ipaddress, vm_with_reset.nic[0].ipaddress)
                        )

        # Check whether the recurring policy has been deleted from the database
        self.debug("Checking whether snapshot rule for VM with isVolatile=True was destroyed \
                   Here we are passing root disk id of vm before reboot which does not exist hence\
                   listing should fail")

        with self.assertRaises(Exception):
            listSnapshotPolicies = SnapshotPolicy.list(
                                        self.apiclient,
                                        volumeid=vm_with_reset_root_disk_id)
            self.assertEqual(
                    validateList(listSnapshotPolicies)[0],
                    PASS,
                    "snapshot policies list validation failed"
                    )
        return
