# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

""" P1 tests for coreos template and vm
"""

from marvin.lib.base import (
    VirtualMachine,
    ServiceOffering,
    Account,
    SSHKeyPair,
    Host, Template)
from marvin.lib.common import (
    get_zone,
    get_template,
    list_routers)
from marvin.lib.utils import (
    cleanup_resources,
    get_hypervisor_type,
    get_process_status)
from marvin.cloudstackTestCase import cloudstackTestCase
# Import System modules
import tempfile
import time
import os
import base64
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient


class TestDeployVmWithCoreosTemplate(cloudstackTestCase):
    """Tests for deploying VM  using coreos template
    """

    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(
            TestDeployVmWithCoreosTemplate,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.service_offering = ServiceOffering.create(
            cls.api_client, services=cls.services["service_offerings"]["medium"])
        cls.account = Account.create(
            cls.api_client, services=cls.services["account"])
        cls.cleanup = [cls.account]
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["coreos"][
            "hypervisor"] = cls.testClient.getHypervisorInfo()
        cls.userdata = '#cloud-config\n\ncoreos:\n  units:\n    - name: docker.service\n      command: start\n    ' \
                       '- name: web.service\n      command: start\n      content: |\n        [Unit]\n        ' \
                       'After=docker.service\n        Requires=docker.service\n        Description=Starts web server ' \
                       'container\n        [Service]\n        TimeoutStartSec=0\n        ' \
                       'ExecStartPre=/usr/bin/docker pull httpd:2.4\n        ' \
                       'ExecStart=/usr/bin/docker run -d -p 8000:80 httpd:2.4'
        cls.services["virtual_machine"]["userdata"] = cls.userdata

        cls.keypair = SSHKeyPair.create(
            cls.api_client,
            name="coreos",
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        cls.debug("Created a new keypair with name: %s" % cls.keypair.name)

        cls.debug("Writing the private key to local file")
        cls.keyPairFilePath = tempfile.gettempdir() + os.sep + cls.keypair.name
        # Clenaup at end of execution
        cls.cleanup.append(cls.keyPairFilePath)

        cls.debug("File path: %s" % cls.keyPairFilePath)

        f = open(cls.keyPairFilePath, "w+")
        f.write(cls.keypair.privatekey)
        f.close()
        os.system("chmod 400 " + cls.keyPairFilePath)
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        return

    def setUp(self):
        self.api_client = self.testClient.getApiClient()
        return

    def tearDown(self):
        # Clean up, terminate the created volumes
        cleanup_resources(self.api_client, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test1_coreos_VM_creation(self):

        self.hypervisor = str(get_hypervisor_type(self.api_client)).lower()
        self.services["virtual_machine"][
            "hypervisor"] = self.hypervisor.upper()
        if self.hypervisor == "vmware":
            self.services["coreos"][
                "url"] = self.services["coreos"]["urlvmware"]
            self.services["coreos"]["format"] = "OVA"
        elif self.hypervisor == "xenserver":
            self.services["coreos"][
                "url"] = self.services["coreos"]["urlxen"]
            self.services["coreos"]["format"] = "VHD"
        elif self.hypervisor == "kvm":
            self.services["coreos"][
                "url"] = self.services["coreos"]["urlkvm"]
            self.services["coreos"]["format"] = "QCOW2"
        elif self.hypervisor == "hyperv":
            self.services["coreos"][
                "url"] = self.services["coreos"]["urlxen"]
            self.services["coreos"]["format"] = "VHD"

        template_created = Template.register(
            self.api_client,
            self.services["coreos"],
            self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid)
        self.assertIsNotNone(template_created, "Template creation failed")
        # Wait for template to download
        template_created.download(self.api_client)
        self.cleanup.append(template_created)
        # Wait for template status to be changed across
        time.sleep(self.services["sleep"])

        self.debug("Deploying instance in the account: %s" % self.account.name)

        virtual_machine = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            templateid=template_created.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            serviceofferingid=self.service_offering.id,
            keypair=self.keypair.name,
            hypervisor=self.hypervisor,
            mode=self.zone.networktype,
            method="POST"

        )

        self.debug("Check if the VM is properly deployed or not?")
        vms = VirtualMachine.list(
            self.api_client,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )
        virtual_machine.stop(self.api_client)
        virtual_machine.update(
            self.api_client,
            userdata=base64.b64encode(
                self.userdata))
        virtual_machine.start(self.api_client)
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running"
        )
        # Wait for docker service to start
        time.sleep(300)

        # Should be able to SSH VM
        try:

            self.debug("SSH into VM: %s" % virtual_machine.ssh_ip)
            cmd = "docker ps"
            ssh = SshClient(virtual_machine.ssh_ip, 22, "core",
                            "", keyPairFiles=self.keyPairFilePath)
            result = ssh.execute(cmd)

        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (virtual_machine.ssh_ip, e)
            )

        res = str(result)
        self.assertEqual(
            res.__contains__("httpd"),
            True,
            "docker web service not started in coreos vm ")

        if self.zone.networktype == "Basic":
            list_router_response = list_routers(
                self.api_client,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.api_Client,
                account=self.account.name,
                domainid=self.account.domainid
            )

        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]
        hosts = Host.list(self.api_client, id=router.hostid)
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list host returns a valid list"
        )
        host = hosts[0]
        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        cmd = "cat /var/www/html/userdata/" + virtual_machine.ipaddress + "/user-data"

        if self.hypervisor.lower() in ('vmware', 'hyperv'):

            try:
                result = get_process_status(
                    self.api_client.connection.mgtSvr,
                    22,
                    self.api_client.connection.user,
                    self.api_client.connection.passwd,
                    router.linklocalip,
                    cmd,
                    hypervisor=self.hypervisor
                )

            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host credentials to check USERDATA")

        else:
            try:
                result = get_process_status(
                    host.ipaddress,
                    22,
                    self.services["configurableData"]["host"]["username"],
                    self.services["configurableData"]["host"]["password"],
                    router.linklocalip, cmd
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host credentials to check router user data")
        res = str(result)
        self.assertEqual(
            res.__contains__("name: docker.service"),
            True,
            "Userdata Not applied Check the failures")

        return
