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
""" tests for host tags (host, service offering, template)
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Account,
                             VirtualMachine,
                             Iso,
                             Host,
                             Template,
                             ServiceOffering,
                             Domain)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_hosts)
from marvin.codes import FAILED, PASS
import time

class TestHostTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHostTags, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
        )
        cls.skip = False
        if cls.template == FAILED:
            cls.skip = True
            return

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
        )

        # Create service offerings, disk offerings etc
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )

        cls.services["iso"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.hosts = list_hosts(
            cls.apiclient,
            zoneid=cls.zone.id,
            type='Routing',
            resourcestate='Enabled'
        )
        cls._cleanup = [
            cls.account,
            cls.service_offering
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            print("Cleanup resources used")
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        if self.skip:
            self.skipTest("skip test")
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        if not isinstance(self.template, Template):
            self.template = Template(self.template.__dict__)
        Template.update(self.template, self.apiclient, templatetag="")
        for host in self.hosts:
            Host.update(self.apiclient, id=host.id, hosttags="")

        return

    def set_host_tag(self, hostid, hosttag):
        Host.update(self.apiclient, id=hostid, hosttags=hosttag)

    def set_template_tag(self, tag):
        if not isinstance(self.template, Template):
            self.template = Template(self.template.__dict__)
        Template.update(self.template, self.apiclient, templatetag=tag)

    @attr(tags=["advanced", "advancedsg", "basic"], required_hardware="false")
    def test_01_template_host_tag(cls):
        """ Test template/host tag
        """
        # Validate the following
        # 1. Update template tag to 'template'. Deploy vm, it should fail
        # 2. Update all hosts tag to 'host'. Deploy vm, it should fail
        # 3. Update template tag to NULL, Deploy vm, it should succeed. stop/start/migrate should work
        # 4. Update template tag to 'host', stop/start/migrate should work
        # 5. Update template tag to 'template'. start/migrate should fail
        # 6. Update a host tag to 'template'. 
        # 6.1 vm should be started on the host. migrate should fail
        # 6.2 start vm on other hosts, it should fail

        # 1. Update template tag to 'template'. Deploy vm, it should fail
        cls.set_template_tag("template")
        try:
            cls.vm_1 = VirtualMachine.create(
                cls.apiclient,
                cls.services["virtual_machine"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
            )
            cls.fail("Deploy vm_1 should fail")
        except Exception as e:
            cls.debug("Failed to deploy vm_1 as expected")

        # 2. Update all hosts tag to 'host'. Deploy vm, it should fail
        for host in cls.hosts:
            cls.set_host_tag(host.id, "host")

        try:
            cls.vm_2 = VirtualMachine.create(
                cls.apiclient,
                cls.services["virtual_machine"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
            )
            cls.fail("Deploy vm_2 should fail")
        except Exception as e:
            cls.debug("Failed to deploy vm_2 as expected")

        # 3. Update template tag to NULL, Deploy vm, it should succeed. stop/start/migrate should work
        cls.set_template_tag("")
        cls.vm_3 = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
        )
        cls.vm_3.stop(cls.apiclient, forced=True)
        cls.vm_3.start(cls.apiclient)
        if len(cls.hosts) > 1:
            cls.vm_3.migrate(cls.apiclient)

        # 4. Update template tag to 'host', stop/start/migrate should work
        cls.set_template_tag("host")
        cls.vm_3.stop(cls.apiclient, forced=True)
        cls.vm_3.start(cls.apiclient)
        if len(cls.hosts) > 1:
            cls.vm_3.migrate(cls.apiclient)

        # 5. Update template tag to 'template'. start/migrate should fail
        cls.set_template_tag("template")
        if len(cls.hosts) > 1:
            try:
                cls.vm_3.migrate(cls.apiclient)
                cls.fail("migrate vm_3 should fail")
            except Exception as e:
                cls.debug("Failed to migrate vm_3 as expected")
        cls.vm_3.stop(cls.apiclient, forced=True)
        try:
            cls.vm_3.start(cls.apiclient)
            cls.fail("start vm_3 should fail")
        except Exception as e:
            cls.debug("Failed to start vm_3 as expected")

        # 6. Update a host tag to 'template'.
        host = cls.hosts[0]
        cls.set_host_tag(host.id, "template")            
        # 6.1 vm should be started on the host. migrate should fail
        cls.vm_3.start(cls.apiclient, hostid = host.id)
        list_vm_response = VirtualMachine.list(
            cls.apiclient,
            id=cls.vm_3.id
        )
        vm_response = list_vm_response[0]
        if vm_response.hostid != host.id:
            cls.fail("vm_3 is not started on %s but on %s" % (host.name, vm_response.hostname))
        if len(cls.hosts) > 1:
            try:
                cls.vm_3.migrate(cls.apiclient)
                cls.fail("migrate vm_3 should fail")
            except Exception as e:
                cls.debug("Failed to migrate vm_3 as expected")
        # 6.2 start vm on other hosts, it should fail
        if len(cls.hosts) > 1:
            host_1 = cls.hosts[1]
            try:
                cls.vm_3.start(cls.apiclient, hostid = host_1.id)
                cls.fail("start vm_3 on %s should fail" % host.name)
            except Exception as e:
                cls.debug("Failed to start vm_3 on %s as expected" % host.name)

