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

"""
Tests of VM Autoscaling
"""

import logging
import time
import datetime

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import stopVirtualMachine

from marvin.lib.base import (Account,
                             Autoscale,
                             AutoScaleCondition,
                             AutoScalePolicy,
                             AutoScaleVmProfile,
                             AutoScaleVmGroup,
                             Configurations,
                             DiskOffering,
                             Domain,
                             Project,
                             ServiceOffering,
                             Template,
                             VirtualMachine,
                             Volume,
                             Zone,
                             Network,
                             NetworkOffering,
                             PublicIPAddress,
                             LoadBalancerRule,
                             VPC,
                             VpcOffering,
                             UserData,
                             SSHKeyPair)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.lib.utils import wait_until

MIN_MEMBER = 1
MAX_MEMBER = 2
DEFAULT_EXPUNGE_VM_GRACE_PERIOD = 60
DEFAULT_DURATION = 120
DEFAULT_INTERVAL = 30
DEFAULT_QUIETTIME = 60
NAME_PREFIX = "AS-VmGroup-"

CONFIG_NAME_DISK_CONTROLLER = "vmware.root.disk.controller"
OS_DEFAULT = "osdefault"

class TestVmAutoScaling(cloudstackTestCase):
    """
    Test VM autoscaling
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestVmAutoScaling,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls.initial_vmware_root_disk_controller = Configurations.list(
            cls.apiclient,
            name=CONFIG_NAME_DISK_CONTROLLER)[0].value
        Configurations.update(cls.apiclient,
                              CONFIG_NAME_DISK_CONTROLLER,
                              OS_DEFAULT)

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls.templatesize = int(cls.template.size / (1024 ** 3))
        cls._cleanup = []

        cls.logger = logging.getLogger("TestVmAutoScaling")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.domain = get_domain(cls.apiclient)

        # 1. Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.service_offering_new = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering_new)

        # 2. Create disk offerings (fixed and custom)
        cls.disk_offering_override = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            disksize=cls.templatesize + 1
        )
        cls._cleanup.append(cls.disk_offering_override)

        cls.disk_offering_custom = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            custom=True
        )
        cls._cleanup.append(cls.disk_offering_custom)

        cls.disk_offering_custom_new = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            custom=True
        )
        cls._cleanup.append(cls.disk_offering_custom_new)

        # 3. Create network offering for isolated networks
        cls.services["isolated_network_offering"]["serviceCapabilityList"] = {
            "Lb": {
                "SupportedLbIsolation": 'dedicated',
                "VmAutoScaling": 'true'
            },
        }
        cls.network_offering_isolated = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"]
        )
        cls._cleanup.append(cls.network_offering_isolated)
        cls.network_offering_isolated.update(cls.apiclient, state='Enabled')

        # 4. Create sub-domain
        cls.sub_domain = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls._cleanup.append(cls.sub_domain)

        # 5. Create regular user
        cls.regular_user = Account.create(
            cls.apiclient,
            cls.services["acl"]["accountD11A"],
            domainid=cls.sub_domain.id
        )
        cls._cleanup.append(cls.regular_user)

        # 5. Create api clients for regular user
        cls.regular_user_user = cls.regular_user.user[0]
        cls.regular_user_apiclient = cls.testClient.getUserApiClient(
            cls.regular_user_user.username, cls.sub_domain.name
        )

        # 7. Create networks for regular user
        cls.services["network"]["name"] = "Test Network Isolated - Regular user - 1"
        cls.user_network_1 = Network.create(
            cls.regular_user_apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            zoneid=cls.zone.id
        )
        cls._cleanup.append(cls.user_network_1)

        cls.services["network"]["name"] = "Test Network Isolated - Regular user - 2"
        cls.user_network_2 = Network.create(
            cls.regular_user_apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            zoneid=cls.zone.id
        )
        cls._cleanup.append(cls.user_network_2)

        # 8. Create SSH Keypairs
        cls.keypair_1 = SSHKeyPair.create(
            cls.regular_user_apiclient,
            name="keypair1"
        )

        cls.keypair_2 = SSHKeyPair.create(
            cls.regular_user_apiclient,
            name="keypair2"
        )

        # 8-2. Register userdata
        cls.apiUserdata = UserData.register(
            cls.apiclient,
            name="ApiUserdata",
            userdata="IyEvYmluL2Jhc2gKCmVjaG8gIkFQSVVzZXJkYXRhIgoK",
            #   #!/bin/bash
            #
            #   echo "APIUserData"
            account=cls.regular_user.name,
            domainid=cls.regular_user.domainid
        )

        # 8-3. Register userdata for template
        cls.templateUserdata = UserData.register(
            cls.apiclient,
            name="TemplateUserdata",
            userdata="IyMgdGVtcGxhdGU6IGppbmphCiNjbG91ZC1jb25maWcKcnVuY21kOgogICAgLSBlY2hvICdrZXkge3sgZHMubWV0YV9kYXRhLmtleTEgfX0nID4+IC9yb290L2luc3RhbmNlX21ldGFkYXRhCgo=",
            #    ## template: jinja
            #    #cloud-config
            #    runcmd:
            #        - echo 'key {{ ds.meta_data.key1 }}' >> /root/instance_metadata
            #
            account=cls.regular_user.name,
            domainid=cls.regular_user.domainid
        )

        # 8-3. Link userdata to template
        cls.template = Template.linkUserDataToTemplate(
            cls.apiclient,
            templateid=cls.template.id,
            userdataid=cls.templateUserdata.userdata.id,
            userdatapolicy="append"
        )

        # 9. Get counters for cpu and memory
        counters = Autoscale.listCounters(
            cls.regular_user_apiclient,
            provider="VirtualRouter"
        )
        for counter in counters:
            if counter.source == 'CPU':
                cls.counter_cpu_id = counter.id
            elif counter.source == 'MEMORY':
                cls.counter_memory_id = counter.id
            elif counter.source == 'VIRTUALROUTER' and counter.value == 'public.network.received.average.mbps':
                cls.counter_network_received_id = counter.id
            elif counter.source == 'VIRTUALROUTER' and counter.value == 'public.network.transmit.average.mbps':
                cls.counter_network_transmit_id = counter.id
            elif counter.source == 'VIRTUALROUTER' and counter.value == 'virtual.network.lb.average.connections':
                cls.counter_lb_connection_id = counter.id

        if cls.hypervisor.lower() == 'vmware':
            cls.counter_cpu_or_memory_id = cls.counter_memory_id
        else:
            cls.counter_cpu_or_memory_id = cls.counter_cpu_id

        # 10. Create AS conditions
        cls.scale_up_condition = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid = cls.counter_cpu_id,
            relationaloperator = "GE",
            threshold = 1
        )
        cls._cleanup.append(cls.scale_up_condition)

        cls.scale_down_condition = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid = cls.counter_cpu_or_memory_id,
            relationaloperator = "LE",
            threshold = 100
        )
        cls._cleanup.append(cls.scale_down_condition)

        # 10.2 Create new AS conditions for VM group update
        cls.new_condition_1 = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid=cls.counter_network_received_id,
            relationaloperator="GE",
            threshold=0
        )
        cls._cleanup.append(cls.new_condition_1)

        cls.new_condition_2 = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid=cls.counter_network_transmit_id,
            relationaloperator="GE",
            threshold=0
        )
        cls._cleanup.append(cls.new_condition_2)

        cls.new_condition_3 = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid=cls.counter_lb_connection_id,
            relationaloperator="GE",
            threshold=0
        )
        cls._cleanup.append(cls.new_condition_3)

        # 11. Create AS policies
        cls.scale_up_policy = AutoScalePolicy.create(
            cls.regular_user_apiclient,
            action='ScaleUp',
            conditionids=','.join([cls.scale_up_condition.id]),
            quiettime=DEFAULT_QUIETTIME,
            duration=DEFAULT_DURATION
        )
        cls._cleanup.append(cls.scale_up_policy)

        cls.scale_down_policy = AutoScalePolicy.create(
            cls.regular_user_apiclient,
            action='ScaleDown',
            conditionids=cls.scale_down_condition.id,
            quiettime=DEFAULT_QUIETTIME,
            duration=DEFAULT_DURATION
        )
        cls._cleanup.append(cls.scale_down_policy)

        # 12. Create AS VM Profile
        cls.otherdeployparams = []
        cls.addOtherDeployParam(cls.otherdeployparams, "overridediskofferingid", cls.disk_offering_override.id)
        cls.addOtherDeployParam(cls.otherdeployparams, "diskofferingid", cls.disk_offering_custom.id)
        cls.addOtherDeployParam(cls.otherdeployparams, "disksize", 3)
        cls.addOtherDeployParam(cls.otherdeployparams, "keypairs", cls.keypair_1.name + "," + cls.keypair_2.name)
        cls.addOtherDeployParam(cls.otherdeployparams, "networkids", cls.user_network_1.id + "," + cls.user_network_2.id)

        cls.autoscaling_vmprofile = AutoScaleVmProfile.create(
            cls.regular_user_apiclient,
            serviceofferingid=cls.service_offering.id,
            zoneid=cls.zone.id,
            templateid=cls.template.id,
            userdata="IyEvYmluL2Jhc2gKCmVjaG8gIlRlc3RVc2VyRGF0YSIKCg==",
            #   #!/bin/bash
            #
            #   echo "TestUserData"
            expungevmgraceperiod=DEFAULT_EXPUNGE_VM_GRACE_PERIOD,
            otherdeployparams=cls.otherdeployparams
        )

        cls._cleanup.append(cls.autoscaling_vmprofile)

        # 13. Acquire Public IP and create LoadBalancer rule
        cls.public_ip_address = PublicIPAddress.create(
            cls.regular_user_apiclient,
            services=cls.services["network"],
            networkid=cls.user_network_1.id
        )

        cls.services["lbrule"]["openfirewall"] = False
        cls.load_balancer_rule = LoadBalancerRule.create(
            cls.regular_user_apiclient,
            cls.services["lbrule"],
            ipaddressid=cls.public_ip_address.ipaddress.id,
            networkid=cls.user_network_1.id
        )
        cls._cleanup.append(cls.load_balancer_rule)

        # 14. Create AS VM Group
        cls.autoscaling_vmgroup = AutoScaleVmGroup.create(
            cls.regular_user_apiclient,
            name=NAME_PREFIX + format(datetime.datetime.now(), '%Y%m%d-%H%M%S'),
            lbruleid=cls.load_balancer_rule.id,
            minmembers=MIN_MEMBER,
            maxmembers=MAX_MEMBER,
            scaledownpolicyids=cls.scale_down_policy.id,
            scaleuppolicyids=cls.scale_up_policy.id,
            vmprofileid=cls.autoscaling_vmprofile.id,
            interval=DEFAULT_INTERVAL
        )

        # 15. Get global config
        check_interval_config = Configurations.list(cls.apiclient, name="autoscale.stats.interval")
        cls.check_interval_seconds = int(check_interval_config[0].value)

        # 16. define VMs not be checked
        cls.excluded_vm_ids = []

    @classmethod
    def message(cls, msg):
        cls.logger.debug("=== " + str(datetime.datetime.now()) + " " + msg)

    @classmethod
    def addOtherDeployParam(cls, otherdeployparams, name, value):
        otherdeployparams.append({
            'name': name,
            'value': value
        })

    @classmethod
    def tearDownClass(cls):
        cls.template = Template.linkUserDataToTemplate(
            cls.apiclient,
            templateid=cls.template.id
        )
        Configurations.update(cls.apiclient,
                              CONFIG_NAME_DISK_CONTROLLER,
                              cls.initial_vmware_root_disk_controller)
        super(TestVmAutoScaling, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestVmAutoScaling, self).tearDown()

    def delete_vmgroup(self, vmgroup, apiclient, cleanup=None, expected=True):
        result = True
        try:
            AutoScaleVmGroup.delete(
                vmgroup,
                apiclient,
                cleanup=cleanup
            )
        except Exception as ex:
            result = False
            if expected:
                self.fail(f"Failed to remove Autoscaling VM Group, but expected to succeed : {ex}")
        if result and not expected:
            self.fail("Autoscaling VM Group is removed successfully, but expected to fail")

    def verifyVmCountAndProfiles(self, vmCount, autoscalevmgroupid=None, autoscalevmprofileid=None, networkid=None, projectid=None):
        if autoscalevmgroupid is None:
            autoscalevmgroupid = self.autoscaling_vmgroup.id

        if autoscalevmprofileid is None:
            autoscalevmprofileid = self.autoscaling_vmprofile.id

        if networkid is None:
            networkid = self.user_network_1.id

        vms = VirtualMachine.list(
            self.regular_user_apiclient,
            autoscalevmgroupid=autoscalevmgroupid,
            projectid=projectid,
            userdata=True,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List virtual machines should return a valid list"
        )

        new_vm_ids = []
        for vm in vms:
            new_vm_ids.append(vm.id)
        for vm_id in self.excluded_vm_ids:
            if vm_id not in new_vm_ids:
                self.logger.debug("VM (id: %s) is not found in VM group" % vm_id)
                self.excluded_vm_ids.remove(vm_id)

        self.assertEqual(
            len(vms),
            vmCount,
            "The number of virtual machines (%s) should be equal to %s" % (len(vms), vmCount)
        )

        for vm in vms:
            if vm.id not in self.excluded_vm_ids:
                self.excluded_vm_ids.append(vm.id)
                self.wait_for_vm_start(vm, projectid)
                self.verifyVmProfile(vm, autoscalevmprofileid, networkid, projectid)

    def verifyVmProfile(self, vm, autoscalevmprofileid, networkid=None, projectid=None):
        self.message("Verifying profiles of new VM %s (%s)" % (vm.name, vm.id))
        datadisksizeInBytes = None
        diskofferingid = None
        rootdisksizeInBytes = None
        sshkeypairs = None

        affinitygroupIdsArray = []
        for affinitygroup in vm.affinitygroup:
            affinitygroupIdsArray.append(affinitygroup.id)
        affinitygroupids = ",".join(affinitygroupIdsArray)

        if vm.diskofferingid:
            diskofferingid = vm.diskofferingid
        if vm.keypairs:
            sshkeypairs = vm.keypairs
        serviceofferingid = vm.serviceofferingid
        templateid = vm.templateid

        networkIdsArray = []
        for nic in vm.nic:
            networkIdsArray.append(nic.networkid)
        networkids = ",".join(networkIdsArray)

        volumes = Volume.list(
            self.regular_user_apiclient,
            virtualmachineid=vm.id,
            projectid=projectid,
            listall=True
        )
        for volume in volumes:
            if volume.type == 'ROOT':
                rootdisksizeInBytes = volume.size
            elif volume.type == 'DATADISK':
                datadisksizeInBytes = volume.size
                diskofferingid = volume.diskofferingid

        vmprofiles_list = AutoScaleVmProfile.list(
            self.regular_user_apiclient,
            listall=True,
            projectid=projectid,
            id=autoscalevmprofileid
        )
        vmprofile = vmprofiles_list[0]
        vmprofile_otherdeployparams = vmprofile.otherdeployparams

        self.logger.debug("vmprofile_otherdeployparams = " + str(vmprofile_otherdeployparams))
        self.logger.debug("templateid = " + templateid)
        self.logger.debug("serviceofferingid = " + serviceofferingid)
        self.logger.debug("rootdisksizeInBytes = " + str(rootdisksizeInBytes))
        self.logger.debug("datadisksizeInBytes = " + str(datadisksizeInBytes))
        self.logger.debug("diskofferingid = " + str(diskofferingid))
        self.logger.debug("sshkeypairs = " + str(sshkeypairs))
        self.logger.debug("networkids = " + networkids)
        self.logger.debug("affinitygroupids = " + affinitygroupids)

        self.assertEquals(templateid, vmprofile.templateid)
        self.assertEquals(serviceofferingid, vmprofile.serviceofferingid)

        if vmprofile_otherdeployparams.rootdisksize:
            self.assertEquals(int(rootdisksizeInBytes), int(vmprofile_otherdeployparams.rootdisksize) * (1024 ** 3))
        elif vmprofile_otherdeployparams.overridediskofferingid:
            self.assertEquals(vmprofile_otherdeployparams.overridediskofferingid, self.disk_offering_override.id)
            self.assertEquals(int(rootdisksizeInBytes), int(self.disk_offering_override.disksize) * (1024 ** 3))
        else:
            self.assertEquals(int(rootdisksizeInBytes), int(self.templatesize) * (1024 ** 3))

        if vmprofile_otherdeployparams.diskofferingid:
            self.assertEquals(diskofferingid, vmprofile_otherdeployparams.diskofferingid)
        if vmprofile_otherdeployparams.disksize:
            self.assertEquals(int(datadisksizeInBytes), int(vmprofile_otherdeployparams.disksize) * (1024 ** 3))

        if vmprofile_otherdeployparams.keypairs:
            self.assertEquals(sshkeypairs, vmprofile_otherdeployparams.keypairs)
        else:
            self.assertIsNone(sshkeypairs)

        if vmprofile_otherdeployparams.networkids:
            self.assertEquals(networkids, vmprofile_otherdeployparams.networkids)
        else:
            self.assertEquals(networkids, networkid)

        if vmprofile_otherdeployparams.affinitygroupids:
            self.assertEquals(affinitygroupids, vmprofile_otherdeployparams.affinitygroupids)
        else:
            self.assertEquals(affinitygroupids, '')

        userdata = None
        userdatadetails = None
        userdataid = None
        if vm.userdata:
            userdata = vm.userdata
        if vm.userdatadetails:
            userdatadetails = vm.userdatadetails
        if vm.userdataid:
            userdataid = vm.userdataid

        if vmprofile.userdataid:
            self.assertEquals(userdataid, vmprofile.userdataid)
        else:
            self.assertIsNone(userdataid)

        if vmprofile.userdatadetails:
            self.assertEquals(userdatadetails, vmprofile.userdatadetails)
        else:
            self.assertIsNone(userdatadetails)

        if vmprofile.userdata:
            self.assertEquals(userdata, vmprofile.userdata)
        else:
            self.assertIsNone(userdata)

    def wait_for_vm_start(self, vm=None, project_id=None):
        """ Wait until vm is Running """
        def check_user_vm_state():
            vms = VirtualMachine.list(
                self.apiclient,
                id=vm.id,
                projectid=project_id,
                userdata=True,
                listall=True
            )
            if isinstance(vms, list):
                if vms[0].state == 'Running':
                    return True, vms[0].state
            return False, vms[0].state

        self.message("Waiting for user VM %s (%s) to be Running" % (vm.name, vm.id))
        res = wait_until(10, 30, check_user_vm_state)
        if not res:
            raise Exception("Failed to wait for user VM %s (%s) to be Running" % (vm.name, vm.id))
        return res

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_scale_up_verify(self):
        """ Verify scale up of AutoScaling VM Group """
        self.message("Running test_01_scale_up_verify")

        # VM count increases from 0 to MIN_MEMBER
        sleeptime = DEFAULT_INTERVAL * MIN_MEMBER + 5
        self.message("Waiting %s seconds for %s VM(s) to be created" % (sleeptime, MIN_MEMBER))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MIN_MEMBER)

        # VM count increases from MIN_MEMBER to MAX_MEMBER
        sleeptime = (self.check_interval_seconds + DEFAULT_DURATION + DEFAULT_QUIETTIME) * (MAX_MEMBER - MIN_MEMBER)
        self.message("Waiting %s seconds for other %s VM(s) to be created" % (sleeptime, (MAX_MEMBER - MIN_MEMBER)))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MAX_MEMBER)


    @attr(tags=["advanced"], required_hardware="false")
    def test_02_update_vmprofile_and_vmgroup(self):
        """ Verify update of AutoScaling VM Group and VM Profile"""
        self.message("Running test_02_update_vmprofile_and_vmgroup")

        vmprofiles_list = AutoScaleVmProfile.list(
            self.regular_user_apiclient,
            listall=True,
            id=self.autoscaling_vmprofile.id
        )
        self.assertEqual(
            isinstance(vmprofiles_list, list),
            True,
            "List autoscale profiles should return a valid list"
        )
        self.assertEqual(
            len(vmprofiles_list) == 1,
            True,
            "The number of autoscale profiles (%s) should be equal to 1" % (len(vmprofiles_list))
        )

        # Create new AS VM Profile
        otherdeployparams_new = []
        self.addOtherDeployParam(otherdeployparams_new, "rootdisksize", self.templatesize + 2)
        self.addOtherDeployParam(otherdeployparams_new, "diskofferingid", self.disk_offering_custom_new.id)
        self.addOtherDeployParam(otherdeployparams_new, "disksize", 5)
        self.addOtherDeployParam(otherdeployparams_new, "keypairs", self.keypair_1.name)
        self.addOtherDeployParam(otherdeployparams_new, "networkids", self.user_network_1.id)

        try:
            Autoscale.updateAutoscaleVMProfile(
                self.regular_user_apiclient,
                id = self.autoscaling_vmprofile.id,
                userdataid=self.apiUserdata.userdata.id,
                userdatadetails=[{"key1": "value2"}],
                serviceofferingid = self.service_offering_new.id,
                expungevmgraceperiod = DEFAULT_EXPUNGE_VM_GRACE_PERIOD + 1,
                otherdeployparams = otherdeployparams_new
            )
            self.fail("Autoscale VM Profile should not be updatable when VM Group is not Disabled")
        except Exception as ex:
            pass

        try:
            Autoscale.updateAutoscaleVMGroup(
                self.regular_user_apiclient,
                id = self.autoscaling_vmprofile.id,
                name=NAME_PREFIX + format(datetime.datetime.now(), '%Y%m%d-%H%M%S'),
                maxmembers = MAX_MEMBER + 1,
                minmembers = MIN_MEMBER + 1,
                interval = DEFAULT_INTERVAL + 1
            )
            self.fail("Autoscale VM Group should not be updatable when VM Group is not Disabled")
        except Exception as ex:
            pass

        self.autoscaling_vmgroup.disable(self.regular_user_apiclient)

        try:
            Autoscale.updateAutoscaleVMProfile(
                self.regular_user_apiclient,
                id = self.autoscaling_vmprofile.id,
                serviceofferingid = self.service_offering_new.id,
                expungevmgraceperiod = DEFAULT_EXPUNGE_VM_GRACE_PERIOD + 1,
                otherdeployparams = otherdeployparams_new
            )
        except Exception as ex:
            self.fail("Autoscale VM Profile should be updatable when VM Group is Disabled")

        try:
            Autoscale.updateAutoscaleVMGroup(
                self.regular_user_apiclient,
                id = self.autoscaling_vmgroup.id,
                name=NAME_PREFIX + format(datetime.datetime.now(), '%Y%m%d-%H%M%S'),
                maxmembers = MAX_MEMBER + 1,
                minmembers = MIN_MEMBER + 1,
                interval = DEFAULT_INTERVAL + 1
            )
        except Exception as ex:
            self.fail("Autoscale VM Group should be updatable when VM Group is Disabled")

        self.autoscaling_vmgroup.enable(self.regular_user_apiclient)

        # VM count increases from MAX_MEMBER to MAX_MEMBER+1
        sleeptime = self.check_interval_seconds + DEFAULT_DURATION + DEFAULT_QUIETTIME
        self.message("Waiting %s seconds for other %s VM(s) to be created" % (sleeptime, 1))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MAX_MEMBER + 1)

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_scale_down_verify(self):
        """ Verify scale down of AutoScaling VM Group """
        self.message("Running test_03_scale_down_verify")

        self.autoscaling_vmgroup.disable(self.regular_user_apiclient)

        policies = Autoscale.listAutoscalePolicies(
            self.regular_user_apiclient,
            action="ScaleUp",
            vmgroupid=self.autoscaling_vmgroup.id
        )
        self.assertEqual(
            isinstance(policies, list),
            True,
            "List autoscale policies should return a valid list"
        )
        self.assertEqual(
            len(policies) == 1,
            True,
            "The number of autoscale policies (%s) should be equal to 1" % (len(policies))
        )
        scale_up_policy = policies[0]

        conditions = Autoscale.listConditions(
            self.regular_user_apiclient,
            policyid=scale_up_policy.id
        )
        self.assertEqual(
            isinstance(conditions, list),
            True,
            "List conditions should return a valid list"
        )

        for condition in conditions:
            if condition.counterid == self.counter_cpu_id:
                Autoscale.updateCondition(
                    self.regular_user_apiclient,
                    id=condition.id,
                    relationaloperator="GT",
                    threshold=101
                )

        policies = Autoscale.listAutoscalePolicies(
            self.regular_user_apiclient,
            action="ScaleDown",
            vmgroupid=self.autoscaling_vmgroup.id
        )
        self.assertEqual(
            isinstance(policies, list),
            True,
            "List autoscale policies should return a valid list"
        )
        self.assertEqual(
            len(policies) == 1,
            True,
            "The number of autoscale policies (%s) should be equal to 1" % (len(policies))
        )
        scale_down_policy = policies[0]

        Autoscale.updateAutoscalePolicy(
            self.regular_user_apiclient,
            id=scale_down_policy.id,
            conditionids=','.join([self.new_condition_1.id, self.new_condition_2.id, self.new_condition_3.id])
        )

        self.autoscaling_vmgroup.enable(self.regular_user_apiclient)

        # VM count decreases from MAX_MEMBER+1 to MIN_MEMBER+1
        sleeptime = (self.check_interval_seconds + DEFAULT_DURATION + DEFAULT_QUIETTIME + DEFAULT_EXPUNGE_VM_GRACE_PERIOD) * (MAX_MEMBER - MIN_MEMBER)
        self.message("Waiting %s seconds for %s VM(s) to be destroyed" % (sleeptime, MAX_MEMBER - MIN_MEMBER))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MIN_MEMBER+1)

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_stop_remove_vm_in_vmgroup(self):
        """ Verify removal of VM in AutoScaling VM Group"""
        self.message("Running test_04_remove_vm_in_vmgroup")

        vms = VirtualMachine.list(
            self.regular_user_apiclient,
            autoscalevmgroupid=self.autoscaling_vmgroup.id,
            userdata=True,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List virtual machines should return a valid list"
        )
        self.assertEqual(
            len(vms) >= MIN_MEMBER,
            True,
            "The number of virtual machines (%s) should be equal to or greater than %s" % (len(vms), MIN_MEMBER)
        )

        vm = vms[0]
        try:
            cmd = stopVirtualMachine.stopVirtualMachineCmd()
            cmd.id = vm.id
            cmd.forced = True
            self.apiclient.stopVirtualMachine(cmd)
            self.fail("VM should not be stopped when VM Group is not Disabled")
        except Exception as ex:
            pass

        try:
            VirtualMachine.delete(vm, self.regular_user_apiclient, expunge=False)
            self.fail("VM should not be destroyed when VM Group is not Disabled")
        except Exception as ex:
            pass

        self.autoscaling_vmgroup.disable(self.regular_user_apiclient)

        try:
            cmd = stopVirtualMachine.stopVirtualMachineCmd()
            cmd.id = vm.id
            cmd.forced = True
            self.apiclient.stopVirtualMachine(cmd)
        except Exception as ex:
            self.fail("VM should be stopped when VM Group is Disabled")

        try:
            VirtualMachine.delete(vm, self.regular_user_apiclient, expunge=False)
        except Exception as ex:
            self.fail("VM should be destroyed when VM Group is Disabled")

        self.verifyVmCountAndProfiles(MIN_MEMBER)

        VirtualMachine.delete(vm, self.apiclient, expunge=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_remove_vmgroup(self):
        """ Verify removal of AutoScaling VM Group"""
        self.message("Running test_05_remove_vmgroup")

        self.delete_vmgroup(self.autoscaling_vmgroup, self.regular_user_apiclient, cleanup=False, expected=False)
        self.delete_vmgroup(self.autoscaling_vmgroup, self.regular_user_apiclient, cleanup=True, expected=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_06_autoscaling_vmgroup_on_project_network(self):
        """ Testing VM autoscaling on project network """
        self.message("Running test_06_autoscaling_vmgroup_on_project_network")

        # Create project
        project = Project.create(
            self.regular_user_apiclient,
            self.services["project"]
        )
        self.cleanup.append(project)

        # Create project network
        self.services["network"]["name"] = "Test Network Isolated - Project"
        project_network = Network.create(
            self.regular_user_apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            projectid=project.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(project_network)

        # Acquire Public IP and create LoadBalancer rule for project
        public_ip_address_project = PublicIPAddress.create(
            self.regular_user_apiclient,
            services=self.services["network"],
            projectid = project.id,
            networkid=project_network.id
        )

        load_balancer_rule_project = LoadBalancerRule.create(
            self.regular_user_apiclient,
            self.services["lbrule"],
            projectid = project.id,
            ipaddressid=public_ip_address_project.ipaddress.id,
            networkid=project_network.id
        )
        self.cleanup.append(load_balancer_rule_project)

        # Create AS conditions for project
        scale_up_condition_project = AutoScaleCondition.create(
            self.regular_user_apiclient,
            projectid = project.id,
            counterid = self.counter_cpu_id,
            relationaloperator = "GE",
            threshold = 1
        )
        self.cleanup.append(scale_up_condition_project)

        scale_down_condition_project = AutoScaleCondition.create(
            self.regular_user_apiclient,
            projectid = project.id,
            counterid = self.counter_cpu_or_memory_id,
            relationaloperator = "LE",
            threshold = 100
        )
        self.cleanup.append(scale_down_condition_project)

        # Create AS policies for project
        scale_up_policy_project = AutoScalePolicy.create(
            self.regular_user_apiclient,
            action='ScaleUp',
            conditionids=scale_up_condition_project.id,
            quiettime=DEFAULT_QUIETTIME,
            duration=DEFAULT_DURATION
        )
        self.cleanup.append(scale_up_policy_project)

        scale_down_policy_project = AutoScalePolicy.create(
            self.regular_user_apiclient,
            action='ScaleDown',
            conditionids=scale_down_condition_project.id,
            quiettime=DEFAULT_QUIETTIME,
            duration=DEFAULT_DURATION
        )
        self.cleanup.append(scale_down_policy_project)

        # Create AS VM Profile for project
        autoscaling_vmprofile_project = AutoScaleVmProfile.create(
            self.regular_user_apiclient,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            templateid=self.template.id,
            expungevmgraceperiod=DEFAULT_EXPUNGE_VM_GRACE_PERIOD,
            projectid = project.id
        )
        self.cleanup.append(autoscaling_vmprofile_project)

        # Create AS VM Group for project
        autoscaling_vmgroup_project = AutoScaleVmGroup.create(
            self.regular_user_apiclient,
            name=NAME_PREFIX + format(datetime.datetime.now(), '%Y%m%d-%H%M%S'),
            lbruleid=load_balancer_rule_project.id,
            minmembers=MIN_MEMBER,
            maxmembers=MAX_MEMBER,
            scaledownpolicyids=scale_down_policy_project.id,
            scaleuppolicyids=scale_up_policy_project.id,
            vmprofileid=autoscaling_vmprofile_project.id,
            interval=DEFAULT_INTERVAL
        )

        self.excluded_vm_ids = []
        # VM count increases from 0 to MIN_MEMBER
        sleeptime = DEFAULT_INTERVAL * MIN_MEMBER + 5
        self.message("Waiting %s seconds for %s VM(s) to be created" % (sleeptime, MIN_MEMBER))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MIN_MEMBER, autoscaling_vmgroup_project.id, autoscaling_vmprofile_project.id,
                                      project_network.id, project.id)

        # VM count increases from MIN_MEMBER to MAX_MEMBER
        sleeptime = (self.check_interval_seconds + DEFAULT_DURATION + DEFAULT_QUIETTIME) * (MAX_MEMBER - MIN_MEMBER)
        self.message("Waiting %s seconds for other %s VM(s) to be created" % (sleeptime, (MAX_MEMBER - MIN_MEMBER)))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MAX_MEMBER, autoscaling_vmgroup_project.id, autoscaling_vmprofile_project.id,
                                      project_network.id, project.id)

        vms = VirtualMachine.list(
            self.regular_user_apiclient,
            autoscalevmgroupid=autoscaling_vmgroup_project.id,
            projectid=project.id,
            userdata=True,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List virtual machines should return a valid list"
        )
        self.assertEqual(
            len(vms),
            MAX_MEMBER,
            "The number of virtual machines (%s) should be equal to %s" % (len(vms), MAX_MEMBER)
        )

        vm = vms[0]
        # Remove a vm from LB, should fail
        try:
            LoadBalancerRule.remove(load_balancer_rule_project, self.regular_user_apiclient, [vm])
            self.fail("VM should not be removed from load balancer rule when VM Group is not Disabled")
        except Exception as ex:
            pass

        # Remove a vm from LB, should succeed when vm group is Disabled
        autoscaling_vmgroup_project.disable(self.regular_user_apiclient)
        try:
            LoadBalancerRule.remove(load_balancer_rule_project, self.regular_user_apiclient, [vm])
        except Exception as ex:
            self.fail("VM should be removed from load balancer rule when VM Group is Disabled")

        self.verifyVmCountAndProfiles(MAX_MEMBER-1, autoscaling_vmgroup_project.id, autoscaling_vmprofile_project.id,
                                      project_network.id, project.id)

        autoscaling_vmgroup_project.enable(self.regular_user_apiclient)

        self.delete_vmgroup(autoscaling_vmgroup_project, self.regular_user_apiclient, cleanup=False, expected=False)
        self.delete_vmgroup(autoscaling_vmgroup_project, self.regular_user_apiclient, cleanup=True, expected=True)

        VirtualMachine.delete(vm, self.regular_user_apiclient, expunge=False)
        VirtualMachine.delete(vm, self.apiclient, expunge=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_07_autoscaling_vmgroup_on_vpc_network(self):
        """ Testing VM autoscaling on vpc network """
        self.message("Running test_07_autoscaling_vmgroup_on_vpc_network")

        # Create vpc offering
        networkOffering = NetworkOffering.list(
            self.apiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(
            networkOffering) > 0, "No VPC based network offering")

        vpcOffering = VpcOffering.list(self.apiclient, name="Default VPC offering")
        self.assertTrue(vpcOffering is not None and len(
            vpcOffering) > 0, "No VPC offerings found")

        self.services["vpc"] = {}
        self.services["vpc"]["name"] = "test-vpc"
        self.services["vpc"]["displaytext"] = "test-vpc"
        self.services["vpc"]["cidr"] = "192.168.0.0/22"

        self.services["vpc_network"] = {}
        self.services["vpc_network"]["name"] = "test-vpc-network"
        self.services["vpc_network"]["displaytext"] = "test-vpc-network"
        self.services["vpc_network"]["netmask"] = "255.255.255.0"
        self.services["vpc_network"]["gateway"] = "192.168.0.1"

        # Create vpc
        vpc = VPC.create(
            self.regular_user_apiclient,
            self.services["vpc"],
            vpcofferingid=vpcOffering[0].id,
            zoneid=self.zone.id
        )
        self.cleanup.append(vpc)

        # Create vpc network
        self.services["network"]["name"] = "Test Network Isolated - VPC"
        vpc_network = Network.create(
            self.regular_user_apiclient,
            self.services["vpc_network"],
            networkofferingid=networkOffering[0].id,
            vpcid=vpc.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(vpc_network)

        # Acquire Public IP and create LoadBalancer rule for vpc
        public_ip_address_vpc = PublicIPAddress.create(
            self.regular_user_apiclient,
            services=self.services["network"],
            vpcid=vpc.id,
            networkid=vpc_network.id
        )

        load_balancer_rule_vpc = LoadBalancerRule.create(
            self.regular_user_apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip_address_vpc.ipaddress.id,
            networkid=vpc_network.id
        )
        self.cleanup.append(load_balancer_rule_vpc)

        # Create AS conditions for vpc
        scale_up_condition_1 = AutoScaleCondition.create(
            self.regular_user_apiclient,
            counterid = self.counter_cpu_or_memory_id,
            relationaloperator = "GE",
            threshold = 1
        )
        self.cleanup.append(scale_up_condition_1)

        scale_up_condition_2 = AutoScaleCondition.create(
            self.regular_user_apiclient,
            counterid=self.counter_network_received_id,
            relationaloperator="GE",
            threshold=0
        )
        self.cleanup.append(scale_up_condition_2)

        scale_up_condition_3 = AutoScaleCondition.create(
            self.regular_user_apiclient,
            counterid=self.counter_network_transmit_id,
            relationaloperator="GE",
            threshold=0
        )
        self.cleanup.append(scale_up_condition_3)

        scale_up_condition_4 = AutoScaleCondition.create(
            self.regular_user_apiclient,
            counterid=self.counter_lb_connection_id,
            relationaloperator="GE",
            threshold=0
        )
        self.cleanup.append(scale_up_condition_4)

        scale_down_condition_vpc = AutoScaleCondition.create(
            self.regular_user_apiclient,
            counterid = self.counter_cpu_or_memory_id,
            relationaloperator = "LE",
            threshold = 100
        )
        self.cleanup.append(scale_down_condition_vpc)

        # Create AS policies for vpc
        scale_up_policy_vpc = AutoScalePolicy.create(
            self.regular_user_apiclient,
            action='ScaleUp',
            conditionids=','.join([scale_up_condition_1.id, scale_up_condition_2.id,
                                   scale_up_condition_3.id, scale_up_condition_4.id]),
            quiettime=DEFAULT_QUIETTIME,
            duration=DEFAULT_DURATION
        )
        self.cleanup.append(scale_up_policy_vpc)

        scale_down_policy_vpc = AutoScalePolicy.create(
            self.regular_user_apiclient,
            action='ScaleDown',
            conditionids=scale_down_condition_vpc.id,
            quiettime=DEFAULT_QUIETTIME,
            duration=DEFAULT_DURATION
        )
        self.cleanup.append(scale_down_policy_vpc)

        # Create AS VM Profile for vpc
        autoscaling_vmprofile_vpc = AutoScaleVmProfile.create(
            self.regular_user_apiclient,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            templateid=self.template.id,
            expungevmgraceperiod=DEFAULT_EXPUNGE_VM_GRACE_PERIOD
        )

        self.cleanup.append(autoscaling_vmprofile_vpc)

        # Create AS VM Group for vpc
        autoscaling_vmgroup_vpc = AutoScaleVmGroup.create(
            self.regular_user_apiclient,
            name=NAME_PREFIX + format(datetime.datetime.now(), '%Y%m%d-%H%M%S'),
            lbruleid=load_balancer_rule_vpc.id,
            minmembers=MIN_MEMBER,
            maxmembers=MAX_MEMBER,
            scaledownpolicyids=scale_down_policy_vpc.id,
            scaleuppolicyids=scale_up_policy_vpc.id,
            vmprofileid=autoscaling_vmprofile_vpc.id,
            interval=DEFAULT_INTERVAL
        )

        self.excluded_vm_ids = []
        # VM count increases from 0 to MIN_MEMBER
        sleeptime = DEFAULT_INTERVAL * MIN_MEMBER + 5
        self.message("Waiting %s seconds for %s VM(s) to be created" % (sleeptime, MIN_MEMBER))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MIN_MEMBER, autoscaling_vmgroup_vpc.id, autoscaling_vmprofile_vpc.id, vpc_network.id)

        # VM count increases from MIN_MEMBER to MAX_MEMBER
        sleeptime = (self.check_interval_seconds + DEFAULT_DURATION + DEFAULT_QUIETTIME) * (MAX_MEMBER - MIN_MEMBER)
        self.message("Waiting %s seconds for other %s VM(s) to be created" % (sleeptime, (MAX_MEMBER - MIN_MEMBER)))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MAX_MEMBER, autoscaling_vmgroup_vpc.id, autoscaling_vmprofile_vpc.id, vpc_network.id)

        autoscaling_vmgroup_vpc.disable(self.regular_user_apiclient)

        try:
            Autoscale.updateAutoscaleVMGroup(
                self.regular_user_apiclient,
                id = autoscaling_vmgroup_vpc.id,
                name=NAME_PREFIX + format(datetime.datetime.now(), '%Y%m%d-%H%M%S'),
                maxmembers = MAX_MEMBER - 1
            )
        except Exception as ex:
            self.fail("Autoscale VM Group should be updatable when VM Group is Disabled")

        autoscaling_vmgroup_vpc.enable(self.regular_user_apiclient)

        # VM count decreases from MAX_MEMBER to MAX_MEMBER - 1
        sleeptime = self.check_interval_seconds + DEFAULT_DURATION + DEFAULT_EXPUNGE_VM_GRACE_PERIOD
        self.message("Waiting %s seconds for other %s VM(s) to be destroyed" % (sleeptime, 1))
        time.sleep(sleeptime)
        self.verifyVmCountAndProfiles(MAX_MEMBER - 1, autoscaling_vmgroup_vpc.id, autoscaling_vmprofile_vpc.id, vpc_network.id)

        self.delete_vmgroup(autoscaling_vmgroup_vpc, self.regular_user_apiclient, cleanup=False, expected=False)
        self.delete_vmgroup(autoscaling_vmgroup_vpc, self.regular_user_apiclient, cleanup=True, expected=True)
