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

""" Deploy VM from ISO with UEFI
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Iso,
                             DiskOffering,
                             Host,
                             )
from marvin.lib.common import (get_zone,
                               get_domain,
                               list_hosts,
                               )
from marvin.codes import PASS
from marvin.sshClient import SshClient
import xml.etree.ElementTree as ET
from lxml import etree

class TestDeployVMFromISOWithUefi(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeployVMFromISOWithUefi, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()

        cls.hypervisor = cls.testClient.getHypervisorInfo()

        if cls.hypervisor != 'kvm':
            raise unittest.SkipTest("Those tests can be run only for KVM hypervisor")

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

        hosts = list_hosts(cls.apiclient, zoneid = cls.zone.id, type="Routing")

        if not cls.isUefiEnabledOnAtLeastOnHost(hosts):
            raise unittest.SkipTest("At least one host should support UEFI")
     
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"]
        )

        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.testdata["disk_offering"]
        )

        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["iso1"]["zoneid"] = cls.zone.id
        cls.testdata["iso3"]["zoneid"] = cls.zone.id
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup = [cls.account, cls.service_offering, cls.disk_offering]

        cls.centos_iso = Iso.create(
            cls.apiclient,
            cls.testdata["iso1"],
            account=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id
        )
        try:
            # Download the ISO
            cls.centos_iso.download(cls.apiclient)
        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (cls.centos_iso.id, e))

        cls.windows_iso = Iso.create(
            cls.apiclient,
            cls.testdata["iso3"],
            account=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id
        )
        try:
            # Download the ISO
            cls.windows_iso.download(cls.apiclient)
        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (cls.windows_iso.id, e))

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor != 'kvm':
            raise self.skipTest("Skipping test case for non-kvm hypervisor")
        return


    @attr(tags=["advanced", "eip", "advancedns", "basic", "sg"], required_hardware="true")
    def test_01_deploy_vm_from_iso_uefi_secure(self):
        """Test Deploy CentOS Virtual Machine from ISO with UEFI Secure
        """

        self.deployVmAndTestUefi(self.centos_iso, 'Secure', 'yes')

        return

    @attr(tags=["advanced", "eip", "advancedns", "basic", "sg"], required_hardware="true")
    def test_02_deploy_vm_from_iso_uefi_legacy(self):
        """Test Deploy CentOS Virtual Machine from ISO with UEFI Legacy mode
        """
        self.deployVmAndTestUefi(self.centos_iso, 'Legacy', 'no')

        return

    @attr(tags=["advanced", "eip", "advancedns", "basic", "sg"], required_hardware="true")
    def test_03_deploy_windows_vm_from_iso_uefi_legacy(self):
        """Test Deploy Windows Virtual Machine from ISO with UEFI Legacy mode
        """
        self.deployVmAndTestUefi(self.windows_iso, 'Legacy', 'no')

        return

    @attr(tags=["advanced", "eip", "advancedns", "basic", "sg"], required_hardware="true")
    def test_04_deploy_windows_vm_from_iso_uefi_secure(self):
        """Test Deploy Windows Virtual Machine from ISO with UEFI Secure mode
        """
        self.deployVmAndTestUefi(self.windows_iso, 'Secure', 'yes')

        return

    def getVirshXML(self, host, instancename):
        self.assertIsNotNone(host, "Host should not be None")

        self.assertIsNotNone(instancename, "Instance name should not be None")

        sshc = SshClient(
            host=host.ipaddress,
            port=22,
            user=self.hostConfig['username'],
            passwd=self.hostConfig['password'])

        virsh_cmd = 'virsh dumpxml %s' % instancename
        xml_res = sshc.execute(virsh_cmd)
        xml_as_str = ''.join(xml_res)
        parser = etree.XMLParser(remove_blank_text=True)
        return ET.fromstring(xml_as_str, parser=parser)

    def checkBootTypeAndMode(self, root, bootmodesecure, isWindowsIso):

        machine = root.find(".os/type").get("machine")

        self.assertEqual(("q35" in machine), True, "The virtual machine is not with UEFI boot type")

        bootmode = root.find(".os/loader").get("secure")

        self.assertEqual((bootmode == bootmodesecure), True, "The VM is not in the right boot mode")

        if isWindowsIso:
            disks = root.findall("devices/disk")
            for disk in disks:
                bus = disk.find("target").get("bus")
                self.debug("bus is %s" % bus)
                self.assertEqual(bus == 'sata', True, "All disks of the VM should be with bus SATA")

    def deployVmAndTestUefi(self, iso, bootmode, bootmodesecure):
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=iso.id,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hypervisor=self.hypervisor,
            boottype='UEFI',
            bootmode=bootmode,
            zoneid=self.zone.id
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])

        hosts = Host.list(self.apiclient, id=self.virtual_machine.hostid)
        if len(hosts) != 1:
            assert False, "Could not find host with id " + self.virtual_machine.hostid

        host = hosts[0]

        isWindowsIso = False
        if "Windows" in iso.ostypename:
            isWindowsIso = True

        instancename = self.virtual_machine.instancename
        virshxml = self.getVirshXML(host, instancename)
        self.checkBootTypeAndMode(virshxml, bootmodesecure, isWindowsIso)

    @classmethod
    def isUefiEnabledOnAtLeastOnHost(cls, hosts):
        for h in hosts:
            if h.ueficapability:
                return True
        return False
