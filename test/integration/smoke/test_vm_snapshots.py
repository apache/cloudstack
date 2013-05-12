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

# Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin.remoteSSHClient import remoteSSHClient

class Services:
    """Test Snapshots Services
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
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 200,  # in MHz
                                    "memory": 256,  # In MBs
                        },
                        "server": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                         "mgmt_server": {
                                    "ipaddress": '1.2.2.152',
                                    "username": "root",
                                    "password": "password",
                                    "port": 22,
                                },
                        "templates": {
                                    "displaytext": 'Template',
                                    "name": 'Template',
                                    "ostype": "CentOS 5.3 (64-bit)",
                                    "templatefilter": 'self',
                                },
                        "test_dir": "/tmp",
                        "random_data": "random.data",
                        "snapshot_name":"TestSnapshot",
                        "snapshot_displaytext":"Test",
                        "ostype": "CentOS 5.3 (64-bit)",
                        "sleep": 60,
                        "timeout": 10,
                        "mode": 'advanced',  # Networking mode: Advanced, Basic
                    }
        
class TestVmSnapshot(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestVmSnapshot, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.services["templates"]["ostypeid"] = template.ostypeid
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["server"],
                                templateid=template.id,
                                accountid=cls.account.name,
                                domainid=cls.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )
        cls.random_data_0 = random_gen(100)
        cls._cleanup = [
                        cls.service_offering,
                        cls.account,
                        ]
        return
    
    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "smoke"])
    def test_01_create_vm_snapshots(self):
        
        try:
            # Login to VM and write data to file system
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "echo %s > %s/%s" % (self.random_data_0, self.services["test_dir"], self.services["random_data"]),
                "cat %s/%s" % (self.services["test_dir"], self.services["random_data"])
                ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" % 
                            self.virtual_machine.ipaddress)
        self.assertEqual(
                        self.random_data_0,
                        result[0],
                        "Check the random data has be write into temp file!"
                        )
        
        time.sleep(self.services["sleep"])
        
        vm_snapshot = VmSnapshot.create(
                                        self.apiclient, 
                                        self.virtual_machine.id, 
                                        "false", 
                                        self.services["snapshot_name"], 
                                        self.services["snapshot_displaytext"]
                                        )
        self.assertEqual(
                        vm_snapshot.state,
                        "Ready",
                        "Check the snapshot of vm is ready!"
                        )
        return
    
    @attr(tags=["advanced", "advancedns", "smoke"])
    def test_02_revert_vm_snapshots(self):
        try:
            ssh_client = self.virtual_machine.get_ssh_client()

            cmds = [
                "rm -rf %s/%s" % (self.services["test_dir"], self.services["random_data"]),
                "ls %s/%s" % (self.services["test_dir"], self.services["random_data"])
                ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" % 
                            self.virtual_machine.ipaddress)
        
        if str(result[0]).index("No such file or directory") == -1:
            self.fail("Check the random data has be delete from temp file!")
        
        time.sleep(self.services["sleep"])
        
        list_snapshot_response = VmSnapshot.list(self.apiclient,vmid=self.virtual_machine.id,listall=True)
        
        self.assertEqual(
                        isinstance(list_snapshot_response, list),
                        True,
                        "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            list_snapshot_response,
                            None,
                            "Check if snapshot exists in ListSnapshot"
                            )
        
        self.assertEqual(
                        list_snapshot_response[0].state,
                        "Ready",
                        "Check the snapshot of vm is ready!"
                        )
        
        VmSnapshot.revertToSnapshot(self.apiclient,list_snapshot_response[0].id)
        
        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.virtual_machine.id
                                            )
        
        self.assertEqual(
                        list_vm_response[0].state,
                        "Stopped",
                        "Check the state of vm is Stopped!"
                        )

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = list_vm_response[0].id
        self.apiclient.startVirtualMachine(cmd)
        
        time.sleep(self.services["sleep"])
        
        try:
            ssh_client = self.virtual_machine.get_ssh_client(reconnect=True)

            cmds = [
                "cat %s/%s" % (self.services["test_dir"], self.services["random_data"])
                ]

            for c in cmds:
                self.debug(c)
                result = ssh_client.execute(c)
                self.debug(result)

        except Exception:
            self.fail("SSH failed for Virtual machine: %s" % 
                            self.virtual_machine.ipaddress)
        
        self.assertEqual(
                        self.random_data_0,
                        result[0],
                        "Check the random data is equal with the ramdom file!"
                        )
    @attr(tags=["advanced", "advancedns", "smoke"])
    def test_03_delete_vm_snapshots(self):
        
        list_snapshot_response = VmSnapshot.list(self.apiclient,vmid=self.virtual_machine.id,listall=True)
        
        self.assertEqual(
                        isinstance(list_snapshot_response, list),
                        True,
                        "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            list_snapshot_response,
                            None,
                            "Check if snapshot exists in ListSnapshot"
                            )
        """
        cmd = deleteVMSnapshot.deleteVMSnapshotCmd()
        cmd.vmsnapshotid = list_snapshot_response[0].id
        self.apiclient.deleteVMSnapshot(cmd)
        """
        VmSnapshot.deleteVMSnapshot(self.apiclient,list_snapshot_response[0].id)
        
        time.sleep(self.services["sleep"]*3)
        
        list_snapshot_response = VmSnapshot.list(self.apiclient,vmid=self.virtual_machine.id,listall=True)
        
        self.assertEqual(
                        list_snapshot_response,
                        None,
                        "Check list vm snapshot has be deleted"
                        )
