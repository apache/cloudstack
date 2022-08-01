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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase

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
                             VirtualMachine,
                             Zone,
                             Network,
                             NetworkOffering,
                             PublicIPAddress,
                             LoadBalancerRule,
                             SSHKeyPair)

from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)

MIN_MEMBER = 1
MAX_MEMBER = 2
DEFAULT_DESTROY_VM_GRACE_PERIOD = 60
DEFAULT_DURATION = 120
DEFAULT_INTERVAL = 30

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

        # 2. Create disk offerings (fixed and custom)
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            disksize=cls.templatesize + 1
        )
        cls._cleanup.append(cls.disk_offering)

        cls.disk_offering_custom = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            custom=True
        )
        cls._cleanup.append(cls.disk_offering_custom)

        # 3. Create network offering for isolated networks
        cls.network_offering_isolated = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"]
        )
        cls.network_offering_isolated.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_isolated)

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

        # 9. Get counters for cpu and memory
        counters = Autoscale.listCounters(
            cls.regular_user_apiclient,
            provider="VirtualRouter"
        )
        for counter in counters:
            if counter.source == 'cpu':
                cls.counter_cpu_id = counter.id
            elif counter.source == 'memory':
                cls.counter_memory_id = counter.id

        # 10. Create AS conditions
        cls.scale_up_condition = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid = cls.counter_cpu_id,
            relationaloperator = "GE",
            threshold = 1
        )

        cls.scale_down_condition = AutoScaleCondition.create(
            cls.regular_user_apiclient,
            counterid = cls.counter_memory_id,
            relationaloperator = "LE",
            threshold = 100
        )

        cls._cleanup.append(cls.scale_up_condition)
        cls._cleanup.append(cls.scale_down_condition)

        # 11. Create AS policies
        cls.scale_up_policy = AutoScalePolicy.create(
            cls.regular_user_apiclient,
            action='ScaleUp',
            conditionids=cls.scale_up_condition.id,
            duration=DEFAULT_DURATION
        )

        cls.scale_down_policy = AutoScalePolicy.create(
            cls.regular_user_apiclient,
            action='ScaleDown',
            conditionids=cls.scale_down_condition.id,
            duration=DEFAULT_DURATION
        )

        cls._cleanup.append(cls.scale_up_policy)
        cls._cleanup.append(cls.scale_down_policy)

        # 12. Create AS VM Profile
        cls.otherdeployparams = []
        cls.addOtherDeployParam("overridediskofferingid", cls.disk_offering.id)
        cls.addOtherDeployParam("diskofferingid", cls.disk_offering_custom.id)
        cls.addOtherDeployParam("disksize", 3)
        cls.addOtherDeployParam("keypairs", cls.keypair_1.name + "," + cls.keypair_2.name)
        cls.addOtherDeployParam("networkids", cls.user_network_1.id + "," + cls.user_network_2.id)

        cls.autoscaling_vmprofile = AutoScaleVmProfile.create(
            cls.regular_user_apiclient,
            serviceofferingid=cls.service_offering.id,
            zoneid=cls.zone.id,
            templateid=cls.template.id,
            destroyvmgraceperiod=DEFAULT_DESTROY_VM_GRACE_PERIOD,
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

        cls._cleanup.append(cls.public_ip_address)

        # 14. Create AS VM Group
        cls.autoscaling_vmgroup = AutoScaleVmGroup.create(
            cls.regular_user_apiclient,
            name="AS-VmGroup-1",
            lbruleid=cls.load_balancer_rule.id,
            minmembers=MIN_MEMBER,
            maxmembers=MAX_MEMBER,
            scaledownpolicyids=cls.scale_down_policy.id,
            scaleuppolicyids=cls.scale_up_policy.id,
            vmprofileid=cls.autoscaling_vmprofile.id,
            interval=DEFAULT_INTERVAL
        )

        cls._cleanup.append(cls.autoscaling_vmgroup)

    @classmethod
    def addOtherDeployParam(cls, name, value):
        cls.otherdeployparams.append({
            'name': name,
            'value': value
        })

    @classmethod
    def tearDownClass(cls):
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

    @attr(tags=["advanced"], required_hardware="false")
    def test_01_scale_up_verify(self):
        """ Verify scale up of AutoScaling VM group """
        self.logger.debug("test_01_scale_up_verify")

        check_interval_config = Configurations.list(self.apiclient, name="autoscale.stats.interval")
        check_interval = check_interval_config[0].value

        time.sleep(int(int(check_interval)/1000 + DEFAULT_INTERVAL))

    @attr(tags=["advanced"], required_hardware="false")
    def test_02_scale_down_verify(self):
        """ Verify scale down of AutoScaling VM group """
        self.logger.debug("test_02_scale_down_verify")

    @attr(tags=["advanced"], required_hardware="false")
    def test_03_update_vmprofile_and_vmgroup(self):
        """ Verify update of AutoScaling VM group and VM profile"""
        self.logger.debug("test_03_update_vmprofile_and_vmgroup")

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_remove_vm_and_vmgroup(self):
        """ Verify removal of AutoScaling VM group and VM"""
        self.logger.debug("test_04_remove_vm_and_vmgroup")

        self.delete_vmgroup(self.autoscaling_vmgroup, self.regular_user_apiclient, cleanup=False, expected=False)
        self.delete_vmgroup(self.autoscaling_vmgroup, self.regular_user_apiclient, cleanup=True, expected=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_05_autoscaling_vmgroup_on_project_network(self):
        """ Testing VM autoscaling on project network """

        # Create project
        self.project = Project.create(
            self.regular_user_apiclient,
            self.services["project"]
        )
        self.cleanup.append(self.project)

        # Create project network
        self.services["network"]["name"] = "Test Network Isolated - Project"
        self.project_network = Network.create(
            self.regular_user_apiclient,
            self.services["network"],
            networkofferingid=self.network_offering_isolated.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.project_network)
