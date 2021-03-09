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
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (updateStoragePool,
                                  resizeVolume,
                                  listCapacity,
                                  addCluster)
from marvin.sshClient import SshClient
from marvin.lib.common import (get_zone,
                               get_template,
                               get_domain,
                               list_volumes,
                               get_pod,
                               is_config_suitable)
from marvin.lib.base import (Domain,
                             Account,
                             Template,
                             VirtualMachine,
                             Volume,
                             DiskOffering,
                             StoragePool,
                             ServiceOffering,
                             Configurations)
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr
import time


class Test42xBugsMgmtSvr(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        try:
            cls._cleanup = []
            cls.testClient = super(Test42xBugsMgmtSvr, cls).getClsTestClient()
            cls.apiClient = cls.api_client = cls.testClient.getApiClient()
            cls.services = cls.testClient.getParsedTestDataConfig()
            cls.hypervisor = cls.testClient.getHypervisorInfo()
            # Get Domain, Zone, Template
            cls.domain = get_domain(cls.api_client)
            cls.zone = get_zone(cls.api_client,
                                cls.testClient.getZoneForTests())
            cls.pod = get_pod(cls.apiClient, zone_id=cls.zone.id)
            cls.template = get_template(
                cls.api_client,
                cls.zone.id,
                cls.services["ostype"]
            )

            cls.services['mode'] = cls.zone.networktype
            cls.services["hypervisor"] = cls.testClient.getHypervisorInfo()
            # Creating Disk offering, Service Offering and Account
            cls.service_offering = ServiceOffering.create(
                cls.apiClient,
                cls.services["service_offerings"]["tiny"]
            )
            cls.account = Account.create(
                cls.api_client,
                cls.services["account"],
                domainid=cls.domain.id
            )
            # Create account
            cls.account_2 = Account.create(
                cls.api_client,
                cls.services["account2"],
                domainid=cls.domain.id
            )

            # Getting authentication for user in newly created Account
            cls.user = cls.account.user[0]
            cls.userapiclient = cls.testClient.getUserApiClient(
                cls.user.username,
                cls.domain.name
            )
            # add objects created in setUpCls to the _cleanup list
            cls._cleanup = [cls.account,
                            cls.account_2,
                            cls.service_offering]

        except Exception as e:
            cls.tearDownClass()
            raise Exception("Warning: Exception in setup : %s" % e)
        return

    def setUp(self):

        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        # Clean up, terminate the created resources
        cleanup_resources(self.apiClient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags=["advanced", "basic", "tested"])
    @attr(required_hardware="false")
    @attr(configuration='apply.allocation.algorithm.to.pods')
    def test_es_1223_apply_algo_to_pods(self):
        """
        @Desc: Test VM creation while "apply.allocation.algorithm.to.pods" is
        set to true
        @Reference: https://issues.apache.org/jira/browse/CLOUDSTACK-4947
        @Steps:
        Step1: Set global configuration "apply.allocation.algorithm.to.pods"
        to true
        Step2: Restart management server
        Step3: Verifying that VM creation is successful
        """
        # Step1:  set global configuration
        # "apply.allocation.algorithm.to.pods" to true
        # Configurations.update(self.apiClient,
        # "apply.allocation.algorithm.to.pods", "true")
        # TODO: restart management server
        if not is_config_suitable(apiclient=self.apiClient,
                                  name='apply.allocation.algorithm.to.pods',
                                  value='true'):
            self.skipTest('apply.allocation.algorithm.to.pods '
                          'should be true. skipping')
        # TODO:Step2: Restart management server
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = self.template.id
        # Step3: Verifying that VM creation is successful
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.services["virtual_machine2"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.cleanup.append(virtual_machine)
        # Verify VM state
        self.assertEqual(
            virtual_machine.state,
            'Running',
            "Check VM state is Running or not"
        )

        # cleanup: set global configuration
        # "apply.allocation.algorithm.to.pods" back to false
        Configurations.update(
            self.apiClient,
            name="apply.allocation.algorithm.to.pods",
            value="false"
        )
        # TODO:cleanup: Restart management server
        return

    @attr(tags=["advanced", "basic", "tested"])
    @attr(required_hardware="false")
    def test_local_storage_data_disk_tag(self):
        """
        @Desc: Test whether tags are honoured while creating
        data disks on local storage
        @Steps:
        This test needs multiple local storages
        Step1: create a tag 'loc' on the local storage
        Step2: create a disk offering with this storage tag 'loc'
        Step3: create a VM and create disk by selecting the disk offering
         created in step2
        step4: check whether the data disk created in step3 is created on
        local storage with tag 'loc'
        """
        if not self.zone.localstorageenabled:
            self.skipTest('Local storage is not enable for this '
                          'zone. skipping')

        local_storages = StoragePool.list(self.apiClient,
                                          zoneid=self.zone.id,
                                          scope='HOST')
        self.assertEqual(
            isinstance(local_storages, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            local_storages,
            None,
            "Check if local storage pools exists in ListStoragePools"
        )

        cmd = updateStoragePool.updateStoragePoolCmd()
        cmd.zoneid = self.zone.id
        cmd.tags = 'loc'
        cmd.id = local_storages[0].id
        self.apiClient.updateStoragePool(cmd)

        self.services["disk_offering"]["storagetype"] = 'local'
        self.services["disk_offering"]["tags"] = 'loc'
        disk_offering = DiskOffering.create(
            self.apiClient,
            self.services["disk_offering"]
        )
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = self.template.id
        # Step3: Verifying that VM creation is successful
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.services["mode"]
        )
        self.cleanup.append(virtual_machine)
        self.cleanup.append(disk_offering)
        # Verify VM state
        self.assertEqual(
            virtual_machine.state,
            'Running',
            "Check VM state is Running or not"
        )
        self.volume = Volume.create(
            self.apiClient,
            self.services["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=disk_offering.id
        )
        virtual_machine.attach_volume(self.apiClient, self.volume)

        self.attached = True
        list_volume_response = Volume.list(
            self.apiClient,
            id=self.volume.id
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        volume = list_volume_response[0]
        self.assertNotEqual(
            volume.virtualmachineid,
            None,
            "Check if volume state (attached) is reflected"
        )

        storage_pool = StoragePool.list(self.apiClient, id=volume.storageid)

        self.assertEqual(
            volume.storagetype,
            'local',
            "Check list storage pool response has local as storage type"
        )

        self.assertEqual(
            storage_pool[0].tags,
            'loc',
            "Check list storage pool response has tag"
        )
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="false")
    def test_es_1236_cloudstack_sccs(self):
        """
        @Desc: Test whether cloudstack-sccs is available on management server
        @Steps:
        Step1: run cloudstack-sccs on management server
        Step2: It should return a commit hash
        """
        # Step1: run cloudstack-sccs on management server
        mgmt_ssh = SshClient(
            self.apiClient.connection.mgtSvr,
            22,
            self.apiClient.connection.user,
            self.apiClient.connection.passwd
        )
        mgmt_ssh.execute("cloudstack-sccs")
        # Step2: It should return a commit hash
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="false")
    @attr(storage="s3")
    def test_es_1863_register_template_s3_domain_admin_user(self):
        """
        @Desc: Test whether cloudstack allows Domain admin or user
        to register a template using S3/Swift object store.
        @Steps:
        Step1: create a Domain and users in it.
        Step2: Register a template as Domain admin.
        Step3: Register a template as Domain user.
        Step4: Template should be registered successfully.
        """
        # Step1: create a Domain and users in it.
        self.newdomain = Domain.create(self.apiClient,
                                       self.services["domain"])

        # create account in the domain
        self.account_domain = Account.create(
            self.apiClient,
            self.services["account"],
            domainid=self.newdomain.id
        )
        self.cleanup.append(self.account_domain)
        self.cleanup.append(self.newdomain)
        # Getting authentication for user in newly created Account in domain
        self.domain_user = self.account_domain.user[0]
        self.domain_userapiclient = self.testClient.getUserApiClient(
            self.domain_user.username, self.newdomain.name
        )

        # Step2: Register a template as Domain admin.
        self.services["templateregister"]["ostype"] = self.services["ostype"]
        self.domain_template = Template.register(
            self.apiClient,
            self.services["templateregister"],
            zoneid=self.zone.id,
            account=self.account_domain.name,
            domainid=self.newdomain.id,
            hypervisor=self.hypervisor
        )
        # Wait for template to download
        self.domain_template.download(self.api_client)

        # Wait for template status to be changed across
        time.sleep(60)
        # Step3: Register a template as Domain user.
        self.domain_user_template = Template.register(
            self.domain_userapiclient,
            self.services["templateregister"],
            zoneid=self.zone.id,
            account=self.account_domain.name,
            domainid=self.newdomain.id,
            hypervisor=self.hypervisor
        )
        # Wait for template to download
        self.domain_user_template.download(self.api_client)

        # Wait for template status to be changed across
        time.sleep(60)

        # TODO: Step4: Template should be registered successfully.
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="true")
    @attr(hypervisor="KVM")
    def test_CLOUDSTACK_6181_stoppedvm_root_resize(self):
        """
        @Desc: Test root volume resize of stopped VM
        @Reference: https://issues.apache.org/jira/browse/CLOUDSTACK-6181
        @Steps:
        Step1: Deploy VM in stopped state (startvm=false),
        resize via 'resizeVolume', start VM. Root is new size.
        """
        # Check whether usage server is running or not

        if self.hypervisor.lower() != 'kvm':
            self.skipTest("Test can be run only on KVM hypervisor")
        # deploy virtual machine in stopped state
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = self.template.id

        # Step3: Verifying that VM creation is successful
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False
        )
        self.cleanup.append(virtual_machine)
        # Verify VM state
        self.assertEqual(
            virtual_machine.state,
            'Stopped',
            "Check VM state is Stopped or not"
        )
        volumes = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )

        self.assertIsNotNone(volumes, "root volume is not returned properly")
        newrootsize = (self.template.size >> 30) + 2
        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volumes[0].id
        cmd.size = newrootsize
        self.apiClient.resizeVolume(cmd)

        virtual_machine.start(self.apiClient)

        volumes_after_resize = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )

        rootvolume = volumes_after_resize[0]
        success = False
        if rootvolume is not None and rootvolume.size == (newrootsize << 30):
            success = True

        self.assertEqual(
            success,
            True,
            "Check if the root volume resized appropriately"
        )
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="true")
    @attr(hypervisor="KVM")
    def test_CLOUDSTACK_6181_vm_root_resize(self):
        """
        @Desc: Test root volume resize of running VM
        @Reference: https://issues.apache.org/jira/browse/CLOUDSTACK-6181
        @Steps:
        Step1: Deploy VM, resize root volume via 'resizeVolume'.
        """
        # Check whether usage server is running or not

        if self.hypervisor.lower() != 'kvm':
            self.skipTest("Test can be run only on KVM hypervisor")
        # deploy virtual machine in stopped state
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = self.template.id

        # Step3: Verifying that VM creation is successful
        virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
        )
        self.cleanup.append(virtual_machine)
        # Verify VM state
        self.assertEqual(
            virtual_machine.state,
            'Running',
            "Check VM state is Running or not"
        )
        volumes = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )

        self.assertIsNotNone(volumes, "root volume is not returned properly")
        newrootsize = (self.template.size >> 30) + 2
        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volumes[0].id
        cmd.size = newrootsize
        self.apiClient.resizeVolume(cmd)

        volumes_after_resize = list_volumes(
            self.apiClient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )

        rootvolume = volumes_after_resize[0]
        success = False
        if rootvolume is not None and rootvolume.size == (newrootsize << 30):
            success = True

        self.assertEqual(
            success,
            True,
            "Check if the root volume resized appropriately"
        )
        return

    @unittest.skip('In progress')
    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="false")
    def test_CLOUDSTACK_5023(self):
        """
        @Desc: Test whether we are able to delete PF rule while
         rabbit mq is collecting usage events.
        @Steps:
        step1. Run Usage server
        step2. Delete a PF rule and check whether it is
        successful and usage event is generated
        Configure RabbitMQ for usage event generation
        """
        # TBA
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="false")
    @attr(configuration='apply.allocation.algorithm.to.pods')
    def test_es_47_list_os_types_win_2012(self):
        """
        @Desc: Test VM creation while "apply.allocation.algorithm.to.pods"
        is set to true
        @Reference: https://issues.apache.org/jira/browse/CLOUDSTACK-4947
        @Steps:
        Step1: register windows 2012 VM template as windows 8 template
        Step2: deploy a VM with windows2012 template and  Verify
        that VM creation is successful

         """

        if not is_config_suitable(apiclient=self.apiClient,
                                  name='apply.allocation.algorithm.to.pods',
                                  value='true'):
            self.skipTest('apply.allocation.algorithm.to.pods '
                          'should be true. skipping')

        # register windows 2012 VM template as windows 8 template
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "windows VM is not supported on %s" %
                self.hypervisor.lower())
        self.win2012_template = Template.register(
            self.apiClient,
            self.services["win2012template"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.domain.id,
            hypervisor=self.hypervisor
        )
        # Wait for template to download
        self.win2012_template.download(self.apiClient)
        self.cleanup.append(self.win2012_template)
        # Wait for template status to be changed across
        time.sleep(60)
        # Deploy
        self.debug("Deploying win 2012 VM in account: %s" % self.account.name)
        self.services["virtual_machine"]["displayname"] = "win2012"
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = self.win2012_template.id
        vm1 = VirtualMachine.create(
            self.apiClient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(vm1)
        # Verify VM state
        self.assertEqual(
            vm1.state,
            'Running',
            "Check VM state is Running or not"
        )
        return

    @attr(tags=["advanced", "basic", "test"])
    @attr(required_hardware="true")
    def test_secondary_storage_stats(self):
        """
        @Desc: Dashboard is not showing correct secondary
        storage statistics
        @Steps:
        Step1: listCapacity api should show correct secondary
        storage statistics
        """
        cmd = listCapacity.listCapacityCmd()
        cmd.type = 6
        cmd.zoneid = self.zone.id
        response = self.apiClient.listCapacity(cmd)

        self.assertEqual(
            isinstance(response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            response,
            None,
            "Check if listCapacity has returned properly"
        )
        self.assertNotEqual(
            response[0].capacitytotal,
            0,
            "check the total capacity of secondary storage returned"
        )
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="false")
    def test_multiple_mgmt_srvr_session_timeout(self):
        """
        @Desc: Check whether mgmt server session times out with in 30s
        @Steps:
        Step1: run 'telnet localhot 8250' on the management server
        and see that it times out with in 30seconds
        """
        # Step1: run cloudstack-sccs on management server
        mgmt_ssh = SshClient(
            self.apiClient.connection.mgtSvr,
            22,
            self.apiClient.connection.user,
            self.apiClient.connection.passwd
        )
        mgmt_ssh.execute("time telnet localhost 8250")

        # Step2: It should return a commit hash
        return

    @attr(tags=["advanced", "basic"])
    @attr(required_hardware="true")
    def test_add_cluster_datacenter_spaces(self):
        """
        @Desc: Add VmWare cluster to the CS with the data center
        name contains space in between
        @Steps:
        Step1: Add VmWare cluster to the CS with the data center
         name contains space in between.
        """
        if self.hypervisor.lower() != 'vmware':
            self.skipTest('Can be run only on vmware zone. skipping')
        cmd = addCluster.addClusterCmd()
        cmd.zoneid = self.zone.id
        cmd.hypervisor = self.hypervisor
        cmd.clustertype = self.services["configurableData"][
            "vmware_cluster"]["clustertype"]
        cmd.podid = self.pod.id
        cmd.username = self.services["configurableData"][
            "vmware_cluster"]["username"]
        cmd.password = self.services["configurableData"][
            "vmware_cluster"]["password"]
        cmd.publicswitchtype = 'vmwaredvs'
        cmd.guestswitchtype = 'vmwaredvs'
        cmd.url = self.services["configurableData"]["vmware_cluster"]["url"]
        cmd.clustername = self.services[
            "configurableData"]["vmware_cluster"]["url"]
        self.apiClient.addCluster(cmd)
        return
