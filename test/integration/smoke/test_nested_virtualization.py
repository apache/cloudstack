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
""" Tests for Nested Virtualization
"""
#Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              get_hypervisor_type,
                              get_process_status)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             NetworkOffering,
                             Configurations,
                             VirtualMachine,
                             Network)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_test_template)
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient
import logging

class TestNestedVirtualization(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestNestedVirtualization, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        
        cls.logger = logging.getLogger('TestNestedVirtualization')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = get_hypervisor_type(cls.apiclient)
        cls.services['mode'] = cls.zone.networktype
        cls.services["isolated_network"]["zoneid"] = cls.zone.id
        cls.domain = get_domain(cls.apiclient)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.account = Account.create(cls.apiclient, services=cls.services["account"])
        cls.template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        cls.isolated_network_offering = NetworkOffering.create(
                                                cls.apiclient,
                                                cls.services["isolated_network_offering"])
        # Enable Isolated Network offering
        cls.isolated_network_offering.update(cls.apiclient, state='Enabled')
        
        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        cls.cleanup = [cls.account]

    @attr(tags=["advanced"], required_hardware="true")
    def test_nested_virtualization_vmware(self):
        """Test nested virtualization on Vmware hypervisor"""
        if self.hypervisor.lower() not in ["vmware"]:
             self.skipTest("Skipping test because suitable hypervisor/host not present")
             
        # 1) Update nested virtualization configurations, if needed
        configs = Configurations.list(self.apiclient, name="vmware.nested.virtualization")
        rollback_nv = False
        rollback_nv_per_vm = False
        for conf in configs:
            if (conf.name == "vmware.nested.virtualization" and conf.value == "false"):
                config_update = Configurations.update(self.apiclient, "vmware.nested.virtualization", "true")
                self.logger.debug("Updated global setting vmware.nested.virtualization to true")
                rollback_nv = True
            elif (conf.name == "vmware.nested.virtualization.perVM" and conf.value == "false"):
                config_update = Configurations.update(self.apiclient, "vmware.nested.virtualization.perVM", "true")
                self.logger.debug("Updated global setting vmware.nested.virtualization.perVM to true")
                rollback_nv_per_vm = True

        try:
            # 2) Deploy a vm
            virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["small"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.services['mode']
            )
            self.assertTrue(virtual_machine is not None, "VM failed to deploy")
            self.assertTrue(virtual_machine.state == 'Running', "VM is not running")
            self.logger.debug("Deployed vm: %s" % virtual_machine.id)

            isolated_network = Network.create(
                self.apiclient,
                self.services["isolated_network"],
                self.account.name,
                self.account.domainid,
                networkofferingid=self.isolated_network_offering.id)

            virtual_machine.stop(self.apiclient)
            virtual_machine.add_nic(self.apiclient, isolated_network.id)
            virtual_machine.start(self.apiclient)

            # 3) SSH into vm
            ssh_client = virtual_machine.get_ssh_client()

            if ssh_client:
                # run ping test
                result = ssh_client.execute("cat /proc/cpuinfo | grep flags")
                self.logger.debug(result)
            else:
                self.fail("Failed to setup ssh connection to %s" % virtual_machine.public_ip)

            # 4) Revert configurations, if needed
            self.rollback_nested_configurations(rollback_nv, rollback_nv_per_vm)

            #5) Check for CPU flags: vmx for Intel and svm for AMD indicates nested virtualization is enabled
            self.assertTrue(result is not None, "Empty result for CPU flags")
            res = str(result)
            self.assertTrue('vmx' in res or 'svm' in res)
        except Exception as e:
            self.debug('Error=%s' % e)
            self.rollback_nested_configurations(rollback_nv, rollback_nv_per_vm)
            raise e

    def rollback_nested_configurations(self, rollback_nv, rollback_nv_per_vm):
        if rollback_nv:
            config_update = Configurations.update(self.apiclient, "vmware.nested.virtualization", "false")
            self.logger.debug("Reverted global setting vmware.nested.virtualization back to false")
        if rollback_nv_per_vm:
            config_update = Configurations.update(self.apiclient, "vmware.nested.virtualization.perVM", "false")
            self.logger.debug("Reverted global setting vmware.nested.virtualization.perVM back to false")

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Cleanup failed with %s" % e)
