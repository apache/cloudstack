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
""" BVT tests for extensions lifecycle functionalities
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listInfrastructure,
                                  listManagementServers)
from marvin.cloudstackException import CloudstackAPIException
from marvin.lib.base import (Extension,
                             Pod,
                             Cluster,
                             Host,
                             Configurations,
                             Template,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             VirtualMachine)
from marvin.lib.common import (get_zone)
from marvin.lib.utils import (random_gen)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
# Import System modules
import logging
from pathlib import Path
import random
import string
import time

_multiprocess_shared_ = True

CONFIG_EXTENSION_PATH_STATE_CHECK_NAME = "extension.path.state.check.interval"

class TestExtensions(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestExtensions, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        cls._cleanup = []
        cls.logger = logging.getLogger('TestExtensions')
        cls.logger.setLevel(logging.DEBUG)

    @classmethod
    def tearDownClass(cls):
        super(TestExtensions, cls).tearDownClass()

    def setUp(self):
        self.changedConfigurations = {}
        self.staticConfigurations = []
        self.cleanup = []

    def tearDown(self):
        restartServer = False
        for config in self.changedConfigurations:
            value = self.changedConfigurations[config]
            logging.info(f"Reverting value of config: {config} to {value}")
            Configurations.update(self.apiclient,
                config,
                value=value)
            if config in self.staticConfigurations:
                restartServer = True
        if restartServer:
            self.restartAllManagementServers()
        super(TestExtensions, self).tearDown()

    def isManagementUp(self):
        try:
            self.apiclient.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

    def getManagementServerIps(self):
        if self.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
            return None
        cmd = listManagementServers.listManagementServersCmd()
        servers = self.apiclient.listManagementServers(cmd)
        active_server_ips = []
        active_server_ips.append(self.mgtSvrDetails["mgtSvrIp"])
        for idx, server in enumerate(servers):
            if server.state == 'Up' and server.ipaddress != self.mgtSvrDetails["mgtSvrIp"]:
                active_server_ips.append(server.ipaddress)
        return active_server_ips

    def restartAllManagementServers(self):
        """Restart all management servers
         Assumes all servers have same username and password"""
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            self.staticConfigurations.clear()
            self.fail(f"MS restarts cannot be done on {self.mgtSvrDetails['mgtSvrIp']}")
            return False
        logging.info("Restarting all management server")
        for idx, server_ip in enumerate(server_ips):
            logging.info(f"Restarting management server #{idx} with IP {server_ip}")
            sshClient = SshClient(
                server_ip,
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"]
            )
            command = "service cloudstack-management stop"
            sshClient.execute(command)
            command = "service cloudstack-management start"
            sshClient.execute(command)
            if idx == 0:
                # Wait before restarting other management servers to make the first as oldest running
                time.sleep(10)

        # Waits for management to come up in 10 mins, when it's up it will continue
        timeout = time.time() + (10 * 60)
        while time.time() < timeout:
            if self.isManagementUp() is True: return True
            time.sleep(5)
        logging.info("Management server did not come up, failing")
        return False

    def changeConfiguration(self, name, value):
        current_config = Configurations.list(self.apiclient, name=name)[0]
        if current_config.value == str(value):
            return
        logging.info(f"Current value for config: {name} is {current_config.value}, changing it to {value}")
        self.changedConfigurations[name] = current_config.value
        if current_config.isdynamic == False:
            self.staticConfigurations.append(name)
        Configurations.update(self.apiclient,
            name,
            value=value)

    def create_cluster(self):
        pod_list = Pod.list(self.apiclient)
        if len(pod_list) <= 0:
            self.fail("No Pods found")
        pod_id = pod_list[0].id
        cluster_services = {
            'clustername': 'C0-Test-' + random_gen(),
            'clustertype': 'CloudManaged'
        }
        self.cluster = Cluster.create(
            self.apiclient,
            cluster_services,
            self.zone.id,
            pod_id,
            'External'
        )
        self.cleanup.append(self.cluster)

    def move_extension_path_locally(self, source, target):
        try:
            Path(source).rename(target)
        except Exception as e:
            self.fail(f"Failed to move extension path on localhost: {str(e)}")

    def move_extension_path(self, path, restore=False):
        logging.info(f"Moving path {path}")
        source = path
        target = f"{path}-backup"
        if restore:
            source = target
            target = path
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            if self.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
                self.move_extension_path_locally(source, target)
                return
            self.fail(f"Extension path cannot be moved on {cls.mgtSvrDetails['mgtSvrIp']}")
        logging.info(f"Moving {path} on all management servers")
        command = f"mv {source} {target}"
        for idx, server_ip in enumerate(server_ips):
            logging.info(f"Moving {path} on management server #{idx} with IP {server_ip}")
            sshClient = SshClient(
                server_ip,
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"]
            )
            sshClient.execute(command)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_create_extension(self):
        name = random_gen()
        type = 'Orchestrator'
        details = [{}]
        details[0]['abc'] = 'xyz'
        self.extension = Extension.create(
            self.apiclient,
            name=name,
            type=type,
            details=details
        )
        self.cleanup.append(self.extension)
        self.assertEqual(
            name,
            self.extension.name,
            "Check extension name failed"
        )
        self.assertEqual(
            type,
            self.extension.type,
            "Check extension type failed"
        )
        self.assertEqual(
            'Enabled',
            self.extension.state,
            "Check extension state failed"
        )
        self.assertTrue(
            self.extension.pathready,
            "Check extension path ready failed"
        )
        extension_details = self.extension.details.__dict__
        for k, v in details[0].items():
            self.assertIn(k, extension_details, f"Key '{k}' should be present in details")
            self.assertEqual(v, extension_details[k], f"Value for key '{k}' should be '{v}'")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_create_extension_type_fail(self):
        type = 'Random'
        try:
            self.extension = Extension.create(
                self.apiclient,
                name=random_gen(),
                type=type
            )
            self.cleanup.append(self.extension)
            self.fail(f"Unknown type: {type} extension created")
        except CloudstackAPIException: pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_create_extension_name_fail(self):
        name = random_gen()
        self.extension = Extension.create(
            self.apiclient,
            name=name,
            type='Orchestrator'
        )
        self.cleanup.append(self.extension)
        try:
            self.extension1 = Extension.create(
                self.apiclient,
                name=name,
                type='Orchestrator'
            )
            self.cleanup.append(self.extension1)
            self.fail(f"Same name: {name} extension created twice")
        except CloudstackAPIException: pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_update_extension(self):
        details = [{}]
        details[0]['abc'] = 'xyz'
        self.extension = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator',
            description='Test description',
            state='Disabled',
            details=details
        )
        self.cleanup.append(self.extension)

        details[0]['bca'] = 'yzx'
        description = 'Updated test description'
        state = 'Enabled'
        updated_extension = self.extension.update(
            self.apiclient,
            description=description,
            state=state,
            details=details)
        self.assertEqual(
            description,
            updated_extension.description,
            "Check extension description"
        )
        self.assertEqual(
            state,
            updated_extension.state,
            "Check extension state"
        )
        self.assertTrue(
            updated_extension.details is not None,
            "Check extension details not None"
        )
        updated_details = updated_extension.details.__dict__
        for k, v in details[0].items():
            self.assertIn(k, updated_details, f"Key '{k}' should be present in updated details")
            self.assertEqual(v, updated_details[k], f"Value for key '{k}' should be '{v}'")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_register_unregister_extension(self):
        self.create_cluster()
        self.extension = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator'
        )
        self.cleanup.append(self.extension)
        registered_extension = self.extension.register(self.apiclient, self.cluster.id, 'Cluster')
        resource_found = False
        if registered_extension is not None and registered_extension.resources is not None and len(registered_extension.resources) > 0:
            for resource in registered_extension.resources:
                if resource.id == self.cluster.id and resource.type == 'Cluster':
                    resource_found = True
                    break
        self.assertTrue(resource_found, "Registered resource not found")
        self.extension.unregister(self.apiclient, self.cluster.id, 'Cluster')

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_register_extension_already_fail(self):
        self.create_cluster()
        self.extension = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator'
        )
        self.cleanup.append(self.extension)
        registered_extension = self.extension.register(self.apiclient,self.cluster.id, 'Cluster')['extension']
        self.extension1 = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator'
        )
        self.cleanup.append(self.extension1)
        try:
            self.extension1.register(self.apiclient, self.cluster.id, 'Cluster')
            self.fail(f"Same cluster: {cluster.id} registered with two extensions")
        except CloudstackAPIException: pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_07_delete_extension_registered_resource_fail(self):
        self.create_cluster()
        self.extension = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator'
        )
        self.cleanup.append(self.extension)
        self.extension.register(self.apiclient, self.cluster.id, 'Cluster')
        try:
            self.extension.delete(self.apiclient, False)
            self.fail("Deleted extensions which is registered to a resource")
        except CloudstackAPIException: pass

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_08_extension_sync(self):
        sync_task_interval = 60
        if self.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
            sync_config = Configurations.list(self.apiclient, name=CONFIG_EXTENSION_PATH_STATE_CHECK_NAME)[0]
            if sync_config.value != str(sync_task_interval):
                self.skipTest("Test running on localhost, MS cannot be restarted to change config")
        self.extension = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator'
        )
        self.assertTrue(self.extension.pathready, "Path not ready")
        self.changeConfiguration(CONFIG_EXTENSION_PATH_STATE_CHECK_NAME, sync_task_interval)
        if len(self.staticConfigurations) > 0:
            self.restartAllManagementServers()
        self.move_extension_path(self.extension.path)
        wait_multiple = 1.5
        wait = wait_multiple * sync_task_interval
        logging.info(f"Waiting for {wait_multiple}x{sync_task_interval} = {wait} seconds for background task to execute")
        time.sleep(wait)
        ext = Extension.list(
            self.apiclient,
            id=self.extension.id
        )[0]
        self.assertFalse(ext.pathready, "Path is still ready")
        self.move_extension_path(self.extension.path, True)
        logging.info(f"Waiting for {wait_multiple}x{sync_task_interval} = {wait} seconds for background task to execute")
        time.sleep(wait)
        ext = Extension.list(
            self.apiclient,
            id=self.extension.id
        )[0]
        self.assertTrue(ext.pathready, "Path not ready")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_09_extension_deploy_vm(self):
        self.create_cluster()
        self.extension = Extension.create(
            self.apiclient,
            name=random_gen(),
            type='Orchestrator'
        )
        self.cleanup.append(self.extension)
        self.extension.register(self.apiclient, self.cluster.id, 'Cluster')
        details = {
            'url': random_gen(),
            'zoneid': self.cluster.zoneid,
            'podid': self.cluster.podid,
            'username': 'External',
            'password': 'External'
        }
        self.host = Host.create(
            self.apiclient,
            self.cluster,
            details,
            hypervisor=self.cluster.hypervisortype
        )
        self.cleanup.append(self.host)
        template_name = "ext-" + random_gen()
        template_data = {
            "name": template_name,
            "displaytext": template_name,
            "format": self.host.hypervisor,
            "hypervisor": self.host.hypervisor,
            "ostype": "Other Linux (64-bit)",
            "url": template_name,
            "requireshvm": "True",
            "ispublic": "True",
            "isextractable": "True",
            "extensionid": self.extension.id
        }
        self.template = Template.register(
            self.apiclient,
            template_data,
            zoneid=self.zone.id,
            hypervisor=self.cluster.hypervisortype
        )
        self.cleanup.append(self.template)
        logging.info("Waiting for 3 seconds for template to be ready")
        time.sleep(3)
        self.compute_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"])
        self.cleanup.append(self.compute_offering)
        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["l2-network_offering"],
        )
        self.cleanup.append(self.network_offering)
        self.network_offering.update(self.apiclient, state='Enabled')
        self.services["network"]["networkoffering"] = self.network_offering.id
        self.l2_network = Network.create(
            self.apiclient,
            self.services["l2-network"],
            zoneid=self.zone.id,
            networkofferingid=self.network_offering.id
        )
        self.cleanup.append(self.l2_network)
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = self.template.id
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.compute_offering.id,
            networkids=[self.l2_network.id]
        )
        self.cleanup.append(self.virtual_machine)
        self.assertEqual(
            self.virtual_machine.state,
            'Running',
            "VM not in Running state"
        )
