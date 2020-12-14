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

# Test from the Marvin - Testing in Python wiki

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete,
# list operations on them
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Template,
                             DiskOffering,
                             Volume,
                             Host,
                             GuestOs)

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_pod

from marvin.sshClient import SshClient

from marvin.codes import FAILED

from nose.plugins.attrib import attr

import xml.etree.ElementTree as ET
import logging


class Templates:
    """Test data for templates
    """

    def __init__(self):
        self.templates = {
            "kvmvirtioscsi": {
                "kvm": {
                    "name": "tiny-kvm-scsi",
                    "displaytext": "virtio-scsi kvm",
                    "format": "qcow2",
                    "hypervisor": "kvm",
                    "ostype": "Other PV Virtio-SCSI (64-bit)",
                    "url": "http://dl.openvm.eu/cloudstack/ubuntu/x86_64/ubuntu-16.04-kvm.qcow2.bz2",
                    "requireshvm": True,
                    "ispublic": True,
                    "passwordenabled": True
                }
            }
        }


class TestDeployVirtioSCSIVM(cloudstackTestCase):
    """
    Test deploy a kvm virtio scsi template
    """

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger('TestDeployVirtioSCSIVM')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        testClient = super(TestDeployVirtioSCSIVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.hypervisorNotSupported = False
        cls.hypervisor = testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        if cls.hypervisor.lower() not in ['kvm']:
            cls.hypervisorNotSupported = True
            return
        kvmvirtioscsi = Templates().templates["kvmvirtioscsi"]
        cls.template = Template.register(
            cls.apiclient,
            kvmvirtioscsi[cls.hypervisor.lower()],
            cls.zone.id,
            hypervisor=cls.hypervisor.lower(),
            domainid=cls.domain.id)
        cls.template.download(cls.apiclient)
        cls._cleanup.append(cls.template)

        if cls.template == FAILED:
            assert False, "get_template() failed to return template"

        cls.services["domainid"] = cls.domain.id
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["zoneid"] = cls.zone.id
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.sparse_disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["sparse_disk_offering"]
        )
        cls._cleanup.append(cls.sparse_disk_offering)

        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            zoneid=cls.zone.id,
            serviceofferingid=cls.service_offering.id,
            diskofferingid=cls.sparse_disk_offering.id,
            mode=cls.zone.networktype
        )

        hosts = Host.list(cls.apiclient, id=cls.virtual_machine.hostid)
        if len(hosts) != 1:
            assert False, "Could not find host with id " + cls.virtual_machine.hostid

        cls.vmhost = hosts[0]

        # Stop VM to reset password
        cls.virtual_machine.stop(cls.apiclient)

        password = cls.virtual_machine.resetPassword(cls.apiclient)
        cls.virtual_machine.username = "ubuntu"
        cls.virtual_machine.password = password

        # Start VM after password reset
        cls.virtual_machine.start(cls.apiclient)
        cls._cleanup.append(cls.virtual_machine)


    @classmethod
    def tearDownClass(cls):
        super(TestDeployVirtioSCSIVM, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        super(TestDeployVirtioSCSIVM, self).tearDown()

    def verifyVirshState(self, diskcount):
        host = self.vmhost.ipaddress
        instancename = self.virtual_machine.instancename
        virshxml = self.getVirshXML(host, instancename)

        root = ET.fromstring(virshxml)

        scsis = root.findall("./devices/controller[@type='scsi']/alias[@name='scsi0']/..")

        self.assertEqual(len(scsis), 1, "SCSI controller not found")

        scsiindex = scsis[0].get('index')
        self.assertNotEqual(scsiindex, None, "Could not find index of SCSI controller")

        # find all scsi disks
        disks = root.findall("./devices/disk[@device='disk']/target[@bus='scsi']/..")

        self.assertEqual(len(disks), diskcount, "Could not find expected number of disks")

        for disk in disks:
            for child in disk:
                if child.tag.lower() == "target":
                    dev = child.get("dev")
                    self.assertTrue(dev is not None and dev.startswith("sd"), "disk dev is invalid")
                elif child.tag.lower() == "address":
                    con = child.get("controller")
                    self.assertEqual(con, scsiindex, "disk controller not equal to SCSI " \
                                                     "controller index")
                elif child.tag.lower() == "driver":
                    discard = child.get("discard")
                    if discard:  # may not be defined by older qemu/libvirt
                        self.assertEqual(discard, "unmap", "discard settings not unmap")

    def verifyGuestState(self, diskcount):
        ssh = self.virtual_machine.get_ssh_client(reconnect=True)
        output = ssh.execute("lspci | grep \"Virtio SCSI\"")
        self.assertTrue(len(output) > 0, "Could not find virtio scsi controller")
        output = ssh.execute("lsblk -rS | grep sd")
        for disk in output:
            self.logger.debug("disk " + disk + " found")

        self.assertEqual(len(output), diskcount,
                         "Could not find appropriate number of scsi disks in guest")

    def getVirshXML(self, host, instancename):
        if host is None:
            self.logger.debug("getVirshXML: host is none")
            return ""
        else:
            self.logger.debug("host is: " + host)
        if instancename is None:
            self.logger.debug("getVirshXML: instancename is none")
            return ""
        else:
            self.logger.debug("instancename is: " + instancename)
        sshc = SshClient(
            host=host,
            port=self.services['configurableData']['host']["publicport"],
            user=self.hostConfig['username'],
            passwd=self.hostConfig['password'])

        ssh = sshc.ssh

        chan = ssh.get_transport().open_session()
        chan.exec_command("virsh dumpxml " + instancename)
        stdout = ""
        while True:
            b = chan.recv(10000)
            if len(b) == 0:
                break
            stdout += b

        stderr = ""
        while True:
            b = chan.recv_stderr(10000)
            if len(b) == 0:
                break
            stderr += b

        xstatus = chan.recv_exit_status()
        chan.close()
        if xstatus != 0:
            raise CommandNonzeroException(xstatus, stderr)

        # rely on sshClient to close ssh
        self.logger.debug("xml is: \n\n%s\n\n" % (stdout))
        return stdout

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_verify_libvirt(self):
        """Test that libvirt properly created domain with scsi controller
        """

        # Validate virsh dumpxml
        if self.hypervisorNotSupported:
            self.skipTest("Hypervisor not supported")

        self.verifyVirshState(2)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_verify_libvirt_after_restart(self):
        """ Verify that libvirt settings are as expected after a VM stop / start
        """

        if self.hypervisorNotSupported:
            self.skipTest("Hypervisor not supported")

        self.virtual_machine.stop(self.apiclient)
        self.virtual_machine.start(self.apiclient)
        self.verifyVirshState(2)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_03_verify_libvirt_attach_disk(self):
        """ Verify that libvirt settings are expected after a disk add
        """
        if self.hypervisorNotSupported:
            self.skipTest("Hypervisor not supported")

        self.volume = Volume.create(
            self.apiclient,
            self.services,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.sparse_disk_offering.id
        )

        self.virtual_machine.attach_volume(
            self.apiclient,
            self.volume
        )

        self.verifyVirshState(3)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_verify_guest_lspci(self):
        """ Verify that guest sees scsi controller and disks
        """

        if self.hypervisorNotSupported:
            self.skipTest("Hypervisor not supported")

        self.verifyGuestState(3)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_05_change_vm_ostype_restart(self):
        """ Update os type to Ubuntu, change vm details rootdiskController
            explicitly to scsi.
        """
        if self.hypervisorNotSupported:
            self.skipTest("Hypervisor not supported")

        self.virtual_machine.stop(self.apiclient)
        ostypes = GuestOs.listMapping(self.apiclient, hypervisor="kvm")
        self.assertTrue(len(ostypes) > 0)

        ostypeid = None
        for ostype in ostypes:
            if ostype.osdisplayname == "Ubuntu 16.04 (64-bit)":
                ostypeid = ostype.ostypeid
                break

        self.assertIsNotNone(ostypeid,
                             "Could not find ostypeid for Ubuntu 16.0.4 (64-bit) mapped to kvm")

        self.virtual_machine.update(self.apiclient, ostypeid=ostypeid,
                                    details=[{"rootDiskController": "scsi"}])

        self.virtual_machine.start(self.apiclient)

        self.verifyVirshState(3)

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_06_verify_guest_lspci_again(self):
        """ Verify that guest sees scsi controller and disks after switching ostype and rdc
        """
        if self.hypervisorNotSupported:
            self.skipTest("Hypervisor not supported")

        self.verifyGuestState(3)


class CommandNonzeroException(Exception):
    def __init__(self, code, stderr):
        self.code = code
        self.stderr = stderr

    def __str__(self):
        return "Status code %d: %s" % (self.code, self.stderr)
