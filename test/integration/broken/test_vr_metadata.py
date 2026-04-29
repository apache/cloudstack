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


# this script will cover VMdeployment  with Userdata tests

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import *
from marvin.lib.utils import (validateList, cleanup_resources)
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.codes import PASS,FAIL

_multiprocess_shared_ = True

class Services:
    def __init__(self):
        self.services = {
            "virtual_machine": {
                "displayname": "TesVM1",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
                },
            "ostype": 'CentOS 5.5 (64-bit)',
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256,
                },
            }

class TestDeployVmWithMetaData(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testclient = super(TestDeployVmWithMetaData, cls).getClsTestClient()
        cls.apiclient = cls.testclient.getApiClient()
        cls._cleanup = []
        #cls.services = Services().services
        cls.services = cls.testclient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.testclient.getZoneForTests())
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )


    @classmethod
    def tearDownClass(cls):
        try:
            cls._cleanup = cls._cleanup[::-1]
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        try:
            self.cleanup = self.cleanup[::-1]
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def migrate_VM(self, vm):
        """Migrates VM to another host, if available"""
        self.debug("+++ Migrating one of the VMs in the created "
                   "VPC Tier network to another host, if available...")
        self.debug("Checking if a host is available for migration...")
        hosts = Host.listForMigration(self.apiclient, virtualmachineid=vm.id)
        if hosts:
            self.assertEqual(isinstance(hosts, list), True,
                             "List hosts should return a valid list"
                             )
            host = hosts[0]
            self.debug("Migrating VM with ID: "
                       "%s to Host: %s" % (vm.id, host.id))
            try:
                vm.migrate(self.apiclient, hostid=host.id)
            except Exception as e:
                self.fail("Failed to migrate instance, %s" % e)
            self.debug("Migrated VM with ID: "
                       "%s to Host: %s" % (vm.id, host.id))
        else:
            self.debug("No host available for migration. "
                       "Test requires at-least 2 hosts")
        return host

    def list_nics(self, vm_id):
        list_vm_res = VirtualMachine.list(self.apiclient, id=vm_id)
        self.assertEqual(validateList(list_vm_res)[0], PASS, "List vms returned invalid response")
        nics = list_vm_res[0].nic
        for nic in nics:
            if nic.type == "Shared":
                nic_res = NIC.list(
                    self.apiclient,
                    virtualmachineid=vm_id,
                    nicid=nic.id
                )
                nic_ip = nic_res[0].ipaddress
                self.assertIsNotNone(nic_ip, "listNics API response does not have the ip address")
            else:
                continue
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_deployVM_verify_metadata_in_VR(self):
        """
        1. Create a network (VR as a provider)
        2. Deploy a VM in the network
        3. Verify VM deployment
        4. From the VM, curl the gateway of the VR to verify the corresponding metadata - hypervisor host name
            if the respective Global level and account level flags are set to true
        """
        # Update global setting for "global.allow.expose.host.hostname"
        Configurations.update(self.apiclient,
                              name="global.allow.expose.host.hostname",
                              value="true"
                              )

        # Update Account level setting
        Configurations.update(self.apiclient,
                              name="account.allow.expose.host.hostname",
                              value="true"
                              )

        # Verify that the above mentioned settings are set to true before proceeding
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='global.allow.expose.host.hostname',
                value='true'):
            self.skipTest('global.allow.expose.host.hostname should be true. skipping')

        if not is_config_suitable(
                apiclient=self.apiclient,
                name='account.allow.expose.host.hostname',
                value='true'):
            self.skipTest('account.allow.expose.host.hostname should be true. skipping')

        self.no_isolate = NetworkOffering.create(
            self.apiclient,
            self.services["isolated_network_offering"]
        )
        self.no_isolate.update(self.apiclient, state='Enabled')
        self.isolated_network = Network.create(
            self.apiclient,
            self.services["network"],
            networkofferingid=self.no_isolate.id,
            zoneid=self.zone.id,
            accountid="admin",
            domainid=1
        )
        self.cleanup.append(self.isolated_network)

        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.isolated_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in the isolated network"
        )
        self.cleanup.append(self.vm)

        ip_addr = self.vm.ipaddress
        self.debug("VM ip address = %s" % ip_addr)

        # Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        vr_res = Router.list(
            self.apiclient,
            networkid=self.isolated_network.id,
            listAll=True
        )
        self.assertEqual(validateList(vr_res)[0], PASS, "List Routers returned invalid response")
        vr_ip = vr_res[0].guestipaddress
        ssh = self.vm.get_ssh_client(ipaddress=ip_addr)
        cmd = "curl http://%s/latest/hypervisor-host-name" % vr_ip
        res = ssh.execute(cmd)
        self.debug("Verifying hypervisor hostname details in the VR")
        self.assertEqual(
            str(res),
            self.vm.hostname,
            "Failed to get the hypervisor host name from VR in isolated network"
        )
        # Reset configuration values to default values i.e., false
        Configurations.update(self.apiclient,
                              name="global.allow.expose.host.hostname",
                              value="false"
                              )

        # Update Account level setting
        Configurations.update(self.apiclient,
                              name="account.allow.expose.host.hostname",
                              value="false"
                              )
        return

    @attr(tags=["advanced"], required_hardware='True')
    def test_deployVM_verify_metadata_in_VR_after_migration(self):
        """
        1. Create a network (VR as a provider)
        2. Deploy a VM in the network
        3. Verify VM deployment
        4. Migrate VM to another host
        4. After migration, from the VM, curl the gateway to verify the corresponding metadata - hypervisor host name
            if the respective Global level and account level flags are set to true
        """
        # Update global setting for "global.allow.expose.host.hostname"
        Configurations.update(self.apiclient,
                              name="global.allow.expose.host.hostname",
                              value="true"
                              )

        # Update Account level setting
        Configurations.update(self.apiclient,
                              name="account.allow.expose.host.hostname",
                              value="true"
                              )

        # Verify that the above mentioned settings are set to true before proceeding
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='global.allow.expose.host.hostname',
                value='true'):
            self.skipTest('global.allow.expose.host.hostname should be true. skipping')

        if not is_config_suitable(
                apiclient=self.apiclient,
                name='account.allow.expose.host.hostname',
                value='true'):
            self.skipTest('Account level account.allow.expose.host.hostname should be true. skipping')

        self.no_isolate = NetworkOffering.create(
            self.apiclient,
            self.services["isolated_network_offering"]
        )
        self.no_isolate.update(self.apiclient, state='Enabled')
        self.isolated_network = Network.create(
            self.apiclient,
            self.services["network"],
            networkofferingid=self.no_isolate.id,
            zoneid=self.zone.id,
            accountid="admin",
            domainid=1
        )
        self.cleanup.append(self.isolated_network)

        self.vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id,
            networkids=[self.isolated_network.id],
        )
        self.assertIsNotNone(
            self.vm,
            "VM creation failed in the isolated network"
        )

        host = self.migrate_VM(self.vm)

        self.cleanup.append(self.vm)

        ip_addr = self.vm.ipaddress
        self.debug("VM ip address = %s" % ip_addr)

        # Verify the retrieved ip address in listNICs API response
        self.list_nics(self.vm.id)
        vr_res = Router.list(
            self.apiclient,
            networkid=self.isolated_network.id,
            listAll=True
        )
        self.assertEqual(validateList(vr_res)[0], PASS, "List Routers returned invalid response")
        vr_ip = vr_res[0].guestipaddress
        ssh = self.vm.get_ssh_client(ipaddress=ip_addr)
        cmd = "curl http://%s/latest/hypervisor-host-name" % vr_ip
        res = ssh.execute(cmd)
        self.debug("Verifying hypervisor hostname details in the VR")
        self.assertEqual(
            str(res),
            host.name,
            "Failed to get the hypervisor host name from VR in isolated network"
        )
        # Reset configuration values to default values i.e., false
        Configurations.update(self.apiclient,
                              name="global.allow.expose.host.hostname",
                              value="false"
                              )

        # Update Account level setting
        Configurations.update(self.apiclient,
                              name="account.allow.expose.host.hostname",
                              value="false"
                              )

        return
