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
""" BVT tests for Virtual Machine Life Cycle
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (recoverVirtualMachine,
                                  destroyVirtualMachine,
                                  attachIso,
                                  detachIso,
                                  provisionCertificate,
                                  updateConfiguration,
                                  migrateVirtualMachine,
                                  migrateVirtualMachineWithVolume,
                                  listNics,
                                  listVolumes)
from marvin.lib.utils import *

from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Host,
                             Iso,
                             Router,
                             Configurations,
                             StoragePool,
                             Volume,
                             DiskOffering,
                             NetworkOffering,
                             Network)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template,
                               get_test_ovf_templates,
                               list_hosts,
                               get_vm_vapp_configs)
from marvin.codes import FAILED, PASS
from nose.plugins.attrib import attr
from marvin.lib.decoratorGenerators import skipTestIf
# Import System modules
import time
import json
from operator import itemgetter

_multiprocess_shared_ = True


class TestSecuredVmMigration(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestSecuredVmMigration, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []

        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() not in ["kvm"]:
            cls.hypervisorNotSupported = True

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][
            0].__dict__
        cls.management_ip = cls.config.__dict__["mgtSvr"][0].__dict__["mgtSvrIp"]

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["iso1"]["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        if cls.hypervisor.lower() in ["kvm"]:
            cls.ensure_all_hosts_are_up()
        super(TestSecuredVmMigration, cls).tearDownClass()

    @classmethod
    def ensure_all_hosts_are_up(cls):
        hosts = Host.list(
            cls.apiclient,
            zoneid=cls.zone.id,
            type='Routing',
            hypervisor='KVM'
        )
        for host in hosts:
            if host.state != "Up":
                SshClient(host.ipaddress, port=22, user=cls.hostConfig["username"], passwd=cls.hostConfig["password"]) \
                    .execute("service cloudstack-agent stop ; \
                              sleep 10 ; \
                              service cloudstack-agent start")
                interval = 5
                retries = 10
                while retries > -1:
                    time.sleep(interval)
                    restarted_host = Host.list(
                        cls.apiclient,
                        id=host.id,
                        type='Routing'
                    )[0]
                    if restarted_host.state == "Up":
                        break
                    retries = retries - 1

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.hypervisorNotSupported:
            self.skipTest("Secured migration is not supported on other than KVM")

        self.hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            hypervisor='KVM',
            state='Up')

        if len(self.hosts) < 2:
            self.skipTest("Requires at least two hosts for performing migration related tests")

        self.secure_all_hosts()
        self.updateConfiguration("ca.plugin.root.auth.strictness", "false")

    def tearDown(self):
        self.secure_all_hosts()
        self.updateConfiguration("ca.plugin.root.auth.strictness", "true")
        super(TestSecuredVmMigration, self).tearDown()

    def get_target_host(self, secured, virtualmachineid):
        target_hosts = Host.listForMigration(self.apiclient,
                                             virtualmachineid=virtualmachineid)
        for host in target_hosts:
            h = list_hosts(self.apiclient, type='Routing', id=host.id)[0]
            if h.details.secured == secured:
                return h

        cloudstackTestCase.skipTest(self, "No target hosts available, skipping test.")

    def check_migration_protocol(self, protocol, host):
        resp = SshClient(host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]) \
            .execute("grep -a listen_%s=1 /etc/libvirt/libvirtd.conf | tail -1" % protocol)

        if protocol not in resp[0]:
            cloudstackTestCase.fail(self, "Libvirt listen protocol expected: '" + protocol + "\n"
                                    "does not match actual: " + resp[0])

    def migrate_and_check(self, vm, src_host, dest_host, proto='tls'):
        """
            Migrates a VM from source host to destination host and checks status
        """
        self.check_migration_protocol(protocol=proto, host=src_host)
        vm.migrate(self.apiclient, hostid=dest_host.id)
        vm_response = VirtualMachine.list(self.apiclient, id=vm.id)[0]
        self.assertEqual(vm_response.hostid, dest_host.id, "Check destination host ID of migrated VM")

    def waitUntilHostInState(self, hostId, state="Up", interval=5, retries=20):
        while retries > -1:
            time.sleep(interval)
            host = Host.list(
                self.apiclient,
                hostid=hostId,
                type='Routing'
            )[0]
            if host.state != state:
                if retries >= 0:
                    retries = retries - 1
                    continue
            else:
                print("Host %s now showing as %s" % (hostId, state))
                return

    def unsecure_host(self, host):
        SshClient(host.ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]) \
            .execute("rm -f /etc/cloudstack/agent/cloud* && \
                      service cloudstack-agent stop ; \
                      service libvirtd stop ; \
                      service libvirt-bin stop ; \
                      sed -i 's/listen_tls.*/listen_tls=0/g' /etc/libvirt/libvirtd.conf && \
                      sed -i 's/listen_tcp.*/listen_tcp=1/g' /etc/libvirt/libvirtd.conf && \
                      sed -i '/.*_file=.*/d' /etc/libvirt/libvirtd.conf && \
                      sed -i 's/vnc_tls.*/vnc_tls=0/g' /etc/libvirt/qemu.conf && \
                      service libvirtd start ; \
                      service libvirt-bin start ; \
                      sleep 30 ; \
                      service cloudstack-agent start")
        time.sleep(30)
        print("Unsecuring Host: %s" % (host.name))
        self.waitUntilHostInState(hostId=host.id, state="Up")
        self.check_connection(host=host, secured='false')
        return host

    def secure_all_hosts(self):
        for host in self.hosts:
            cmd = provisionCertificate.provisionCertificateCmd()
            cmd.hostid = host.id
            cmd.reconnect = True
            self.apiclient.provisionCertificate(cmd)

        for host in self.hosts:
            print("Securing Host %s" % host.name)
            self.waitUntilHostInState(hostId=host.id, state="Up")
            self.check_connection(secured='true', host=host)

    def deploy_vm(self, origin_host):
        return VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            mode=self.services["mode"],
            hostid=origin_host.id)

    def check_connection(self, secured, host, retries=20, interval=6):

        while retries > -1:
            time.sleep(interval)
            host = Host.list(
                self.apiclient,
                zoneid=self.zone.id,
                hostid=host.id,
                type='Routing'
            )[0]
            if host.details.secured != secured:
                if retries >= 0:
                    retries = retries - 1
                    continue
            else:
                return

        raise Exception("Host detail 'secured' was expected: " + secured +
                        ", actual is: " + host.details.secured)

    def updateConfiguration(self, name, value):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value
        self.apiclient.updateConfiguration(cmd)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_01_secure_vm_migration(self):
        """Test secure VM migration"""
        # Validate the following
        # 1. Environment has enough hosts for migration
        # 2. DeployVM on suitable host (with another host in the cluster)
        # 3. Migrate the VM and assert migration successful

        src_host = self.hosts[0]
        vm = self.deploy_vm(src_host)
        self.cleanup.append(vm)

        self.debug("Securing Host(s)")
        dest_host = self.get_target_host(secured='true', virtualmachineid=vm.id)
        self.migrate_and_check(vm, src_host, dest_host)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_02_unsecure_vm_migration(self):
        """Test Non-secured VM Migration
        """
        # Validate the following
        # 1. Prepare 2 hosts to run in non-secured more
        # 2. DeployVM on suitable host (with another host in the cluster)
        # 3. Migrate the VM and assert migration successful

        for host in self.hosts:
            self.unsecure_host(host)

        src_host = self.hosts[0]
        vm = self.deploy_vm(src_host)
        self.cleanup.append(vm)

        dest_host = self.get_target_host(secured='false', virtualmachineid=vm.id)
        self.migrate_and_check(vm, src_host, dest_host, proto='tcp')

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_03_secured_to_nonsecured_vm_migration(self):
        """Test destroy Virtual Machine
        """
        # Validate the following
        # 1. Makes one of the hosts non-secured
        # 2. Deploys a VM to a Secured host
        # 3. Migrates the VM to the non-secured host via TLS, and ensure exception

        unsecure_host = self.unsecure_host(self.hosts[0])
        secure_host = self.hosts[1]

        vm = self.deploy_vm(secure_host)
        self.cleanup.append(vm)

        try:
            self.migrate_and_check(vm, secure_host, unsecure_host, proto='tls')
        except Exception:
            pass
        else:
            self.fail("Migration succeeded, instead it should fail")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg", "security"], required_hardware="false")
    def test_04_nonsecured_to_secured_vm_migration(self):
        """Test Non-secured VM Migration
        """
        # Validate the following
        # 1. Makes one of the hosts non-secured
        # 2. Deploys a VM to the non-secured host
        # 3. Migrates the VM to the non-secured host via TCP, and ensure exception

        unsecure_host = self.unsecure_host(self.hosts[0])
        secure_host = self.hosts[1]

        vm = self.deploy_vm(unsecure_host)
        self.cleanup.append(vm)

        try:
            self.migrate_and_check(vm, unsecure_host, secure_host, proto='tcp')
        except Exception:
            pass
        else:
            self.fail("Migration succeeded, instead it should fail")
