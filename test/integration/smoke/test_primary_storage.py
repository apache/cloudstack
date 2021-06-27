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
""" BVT tests for Primary Storage
"""

# Import System modules
# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.lib.utils import *
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestPrimaryStorageServices(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.services = self.testClient.getParsedTestDataConfig()
        self.cleanup = []
        # Get Zone and pod
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.domain = get_domain(self.apiclient)
        self.template = get_suitable_test_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"],
            self.hypervisor
        )
        if self.template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % self.services["ostype"]

        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_primary_storage_nfs(self):
        """Test primary storage pools - XEN, KVM, VMWare. Not Supported for hyperv
        """

        if self.hypervisor.lower() in ["hyperv"]:
            raise self.skipTest("NFS primary storage not supported for Hyper-V")

        # Validate the following:
        # 1. List Clusters
        # 2. verify that the cluster is in 'Enabled' allocation state
        # 3. verify that the host is added successfully and
        #    in Up state with listHosts api response

        # Create NFS storage pools with on XEN/KVM/VMWare clusters

        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        assert isinstance(clusters, list) and len(clusters) > 0
        for cluster in clusters:
            # Host should be present before adding primary storage
            list_hosts_response = list_hosts(
                self.apiclient,
                clusterid=cluster.id
            )
            self.assertEqual(
                isinstance(list_hosts_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_hosts_response),
                0,
                "Check list Hosts in the cluster: " + cluster.name
            )

            storage = StoragePool.create(self.apiclient,
                                         self.services["nfs"],
                                         clusterid=cluster.id,
                                         zoneid=self.zone.id,
                                         podid=self.pod.id
                                         )
            self.cleanup.append(storage)

            self.debug("Created storage pool in cluster: %s" % cluster.id)

            self.assertEqual(
                storage.state,
                'Up',
                "Check primary storage state "
            )

            self.assertEqual(
                storage.type,
                'NetworkFilesystem',
                "Check storage pool type "
            )

            # Verify List Storage pool Response has newly added storage pool
            storage_pools_response = list_storage_pools(
                self.apiclient,
                id=storage.id,
            )
            self.assertEqual(
                isinstance(storage_pools_response, list),
                True,
                "Check list response returns a valid list"
            )
            self.assertNotEqual(
                len(storage_pools_response),
                0,
                "Check list Hosts response"
            )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                storage_response.id,
                storage.id,
                "Check storage pool ID"
            )
            self.assertEqual(
                storage.type,
                storage_response.type,
                "Check storage pool type "
            )
            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []
            return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_01_primary_storage_iscsi(self):
        """Test primary storage pools - XEN. Not Supported for kvm,hyperv,vmware
        """

        if self.hypervisor.lower() in ["kvm", "hyperv", "vmware", "lxc"]:
            raise self.skipTest("iscsi primary storage not supported on kvm, VMWare, Hyper-V, or LXC")

        if not self.services["configurableData"]["iscsi"]["url"]:
            raise self.skipTest("iscsi test storage url not setup, skipping")

        # Validate the following:
        # 1. List Clusters
        # 2. verify that the cluster is in 'Enabled' allocation state
        # 3. verify that the host is added successfully and
        #    in Up state with listHosts api response

        # Create iSCSI storage pools with on XEN/KVM clusters
        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        assert isinstance(clusters, list) and len(clusters) > 0
        for cluster in clusters:
            # Host should be present before adding primary storage
            list_hosts_response = list_hosts(
                self.apiclient,
                clusterid=cluster.id
            )
            self.assertEqual(
                isinstance(list_hosts_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_hosts_response),
                0,
                "Check list Hosts in the cluster: " + cluster.name
            )

            storage = StoragePool.create(self.apiclient,
                                         self.services["configurableData"]["iscsi"],
                                         clusterid=cluster.id,
                                         zoneid=self.zone.id,
                                         podid=self.pod.id
                                         )
            self.cleanup.append(storage)

            self.debug("Created storage pool in cluster: %s" % cluster.id)

            self.assertEqual(
                storage.state,
                'Up',
                "Check primary storage state "
            )

            self.assertEqual(
                storage.type,
                'IscsiLUN',
                "Check storage pool type "
            )

            # Verify List Storage pool Response has newly added storage pool
            storage_pools_response = list_storage_pools(
                self.apiclient,
                id=storage.id,
            )
            self.assertEqual(
                isinstance(storage_pools_response, list),
                True,
                "Check list response returns a valid list"
            )
            self.assertNotEqual(
                len(storage_pools_response),
                0,
                "Check list Hosts response"
            )

            storage_response = storage_pools_response[0]
            self.assertEqual(
                storage_response.id,
                storage.id,
                "Check storage pool ID"
            )
            self.assertEqual(
                storage.type,
                storage_response.type,
                "Check storage pool type "
            )
            # Call cleanup for reusing primary storage
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanup = []

        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_add_primary_storage_disabled_host(self):
        """Test add primary storage pool with disabled host
        """

        # Disable a host
        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        assert isinstance(clusters, list) and len(clusters) > 0
        for cluster in clusters:

            list_hosts_response = list_hosts(
                self.apiclient,
                clusterid=cluster.id,
                type="Routing"
            )
            assert isinstance(list_hosts_response, list)
            if len(list_hosts_response) < 2:
                continue
            selected_cluster = cluster
            selected_host = list_hosts_response[0]
            Host.update(self.apiclient, id=selected_host.id, allocationstate="Disable")

            # create a pool
            storage_pool_2 = StoragePool.create(
                self.apiclient,
                self.services["nfs2"],
                clusterid=selected_cluster.id,
                zoneid=self.zone.id,
                podid=self.pod.id
            )
            # self.cleanup.append(storage_pool_2)

            # Enable host and disable others
            Host.update(self.apiclient, id=selected_host.id, allocationstate="Enable")
            for host in list_hosts_response:
                if (host.id == selected_host.id):
                    continue
                Host.update(self.apiclient, id=host.id, allocationstate="Disable")

            # put other pools in maintenance
            storage_pool_list = StoragePool.list(self.apiclient, zoneid=self.zone.id)
            for pool in storage_pool_list:
                if (pool.id == storage_pool_2.id):
                    continue
                StoragePool.update(self.apiclient, id=pool.id, enabled=False)

            # deployvm
            try:
                # Create Account
                account = Account.create(
                    self.apiclient,
                    self.services["account"],
                    domainid=self.domain.id
                )

                service_offering = ServiceOffering.create(
                    self.apiclient,
                    self.services["service_offerings"]["tiny"]
                )
                self.cleanup.append(service_offering)

                self.virtual_machine = VirtualMachine.create(
                    self.apiclient,
                    self.services["virtual_machine"],
                    accountid=account.name,
                    zoneid=self.zone.id,
                    domainid=self.domain.id,
                    templateid=self.template.id,
                    serviceofferingid=service_offering.id
                )
                self.cleanup.append(self.virtual_machine)
                self.cleanup.append(account)
            finally:
                # cancel maintenance
                for pool in storage_pool_list:
                    if (pool.id == storage_pool_2.id):
                        continue
                    StoragePool.update(self.apiclient, id=pool.id, enabled=True)
                # Enable all hosts
                for host in list_hosts_response:
                    if (host.id == selected_host.id):
                        continue
                    Host.update(self.apiclient, id=host.id, allocationstate="Enable")

                cleanup_resources(self.apiclient, self.cleanup)
                self.cleanup = []
                StoragePool.enableMaintenance(self.apiclient, storage_pool_2.id)
                time.sleep(30);
                cmd = deleteStoragePool.deleteStoragePoolCmd()
                cmd.id = storage_pool_2.id
                cmd.forced = True
                self.apiclient.deleteStoragePool(cmd)

        return


class StorageTagsServices:
    """Test Storage Tags Data Class.
    """

    def __init__(self):
        self.storage_tags = {
            "a": "NFS-A",
            "b": "NFS-B"
        }


class TestStorageTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger('TestStorageTags')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        test_case = super(TestStorageTags, cls)
        testClient = test_case.getClsTestClient()
        cls.config = test_case.getClsConfig()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if cls.template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["storage_tags"] = StorageTagsServices().storage_tags

        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() in ["hyperv"]:
            cls.hypervisorNotSupported = True
        cls._cleanup = []

        if not cls.hypervisorNotSupported:
            cls.clusters = list_clusters(
                cls.apiclient,
                zoneid=cls.zone.id
            )
            assert isinstance(cls.clusters, list) and len(cls.clusters) > 0

            # Create PS with Storage Tag
            cls.storage_pool_1 = StoragePool.create(cls.apiclient,
                                                    cls.services["nfs"],
                                                    clusterid=cls.clusters[0].id,
                                                    zoneid=cls.zone.id,
                                                    podid=cls.pod.id,
                                                    tags=cls.services["storage_tags"]["a"]
                                                    )
            # PS not appended to _cleanup, it is removed on tearDownClass before cleaning up resources
            assert cls.storage_pool_1.state == 'Up'
            storage_pools_response = list_storage_pools(cls.apiclient,
                                                        id=cls.storage_pool_1.id)
            assert isinstance(storage_pools_response, list) and len(storage_pools_response) > 0
            storage_response = storage_pools_response[0]
            assert storage_response.id == cls.storage_pool_1.id and storage_response.type == cls.storage_pool_1.type

            # Create Service Offerings with different Storage Tags
            cls.service_offering_1 = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"],
                tags=cls.services["storage_tags"]["a"]
            )
            cls._cleanup.append(cls.service_offering_1)
            cls.service_offering_2 = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"],
                tags=cls.services["storage_tags"]["b"]
            )
            cls._cleanup.append(cls.service_offering_2)

            # Create Disk Offerings with different Storage Tags
            cls.disk_offering_1 = DiskOffering.create(
                cls.apiclient,
                cls.services["disk_offering"],
                tags=cls.services["storage_tags"]["a"]
            )
            cls._cleanup.append(cls.disk_offering_1)
            cls.disk_offering_2 = DiskOffering.create(
                cls.apiclient,
                cls.services["disk_offering"],
                tags=cls.services["storage_tags"]["b"]
            )
            cls._cleanup.append(cls.disk_offering_2)

            # Create Account
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            # Create VM-1 with using Service Offering 1
            cls.virtual_machine_1 = VirtualMachine.create(
                cls.apiclient,
                cls.services["virtual_machine"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                templateid=cls.template.id,
                serviceofferingid=cls.service_offering_1.id,
                hypervisor=cls.hypervisor,
                mode=cls.zone.networktype
            )
            # VM-1 not appended to _cleanup, it is expunged on tearDownClass before cleaning up resources

        return

    @classmethod
    def tearDownClass(cls):
        try:
            # First expunge vm, so PS can be cleaned up
            cls.virtual_machine_1.delete(cls.apiclient)
            time.sleep(60)

            # Force delete primary storage
            cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
            cmd.id = cls.storage_pool_1.id
            cls.apiclient.enableStorageMaintenance(cmd)
            time.sleep(45)
            cmd = deleteStoragePool.deleteStoragePoolCmd()
            cmd.id = cls.storage_pool_1.id
            cmd.forced = True
            cls.apiclient.deleteStoragePool(cmd)
            time.sleep(30)
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Cleanup failed with %s" % e)

    def setUp(self):
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_01_deploy_vms_storage_tags(self):
        """Test Deploy VMS using different Service Offerings with Storage Tags
        """

        # Save cleanup size before trying to deploy VM-2
        cleanup_size = len(self.cleanup)

        # Try deploying VM-2 using CO-2 -> Should fail to find storage and fail deployment
        try:
            self.virtual_machine_2 = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                templateid=self.template.id,
                serviceofferingid=self.service_offering_2.id,
                hypervisor=self.hypervisor
            )
            self.cleanup.append(self.virtual_machine_2)
        except Exception as e:
            self.debug("Expected exception %s: " % e)

        self.debug("Asssert that vm2 was not deployed, so it couldn't be appended to cleanup")
        self.assertEqual(cleanup_size, len(self.cleanup))

        # Create V-1 using DO-1
        self.volume_1 = Volume.create(
            self.apiclient,
            self.services,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_1.id
        )
        self.cleanup.append(self.volume_1)

        # Create V-2 using DO-2
        self.volume_2 = Volume.create(
            self.apiclient,
            self.services,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering_2.id
        )
        self.cleanup.append(self.volume_2)

        # Try attaching V-2 to VM-1 -> Should fail finding storage and fail attachment
        try:
            self.virtual_machine_1.attach_volume(
                self.apiclient,
                self.volume_2
            )
        except Exception as e:
            self.debug("Expected exception %s: " % e)

        vm_1_volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='DATADISK',
            listall=True
        )
        self.debug("VM-1 Volumes: %s" % vm_1_volumes)
        self.assertEqual(None, vm_1_volumes, "Check that volume V-2 has not been attached to VM-1")

        # Attach V_1 to VM_1
        self.virtual_machine_1.attach_volume(self.apiclient, self.volume_1)
        vm_1_volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='DATADISK',
            listall=True
        )
        self.debug("VM-1 Volumes: %s" % vm_1_volumes)
        self.assertEqual(vm_1_volumes[0].id, self.volume_1.id, "Check that volume V-1 has been attached to VM-1")
        self.virtual_machine_1.detach_volume(self.apiclient, self.volume_1)

        return

    def check_storage_pool_tag(self, poolid, tag):
        cmd = listStorageTags.listStorageTagsCmd()
        storage_tags_response = self.apiclient.listStorageTags(cmd)
        pool_tags = [x for x in storage_tags_response if x.poolid == poolid]
        self.assertEqual(1, len(pool_tags), "Check storage tags size")
        self.assertEqual(tag, pool_tags[0].name, "Check storage tag on storage pool")

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_02_edit_primary_storage_tags(self):
        """ Test Edit Storage Tags
        """

        qresultset = self.dbclient.execute(
            "select id from storage_pool where uuid = '%s';"
            % str(self.storage_pool_1.id)
        )
        self.assertEqual(1, len(qresultset), "Check DB Query result set")
        qresult = qresultset[0]
        storage_pool_db_id = qresult[0]

        self.check_storage_pool_tag(storage_pool_db_id, self.services["storage_tags"]["a"])

        # Update Storage Tag
        StoragePool.update(
            self.apiclient,
            id=self.storage_pool_1.id,
            tags=self.services["storage_tags"]["b"]
        )

        self.check_storage_pool_tag(storage_pool_db_id, self.services["storage_tags"]["b"])

        # Revert Storage Tag
        StoragePool.update(
            self.apiclient,
            id=self.storage_pool_1.id,
            tags=self.services["storage_tags"]["a"]
        )

        self.check_storage_pool_tag(storage_pool_db_id, self.services["storage_tags"]["a"])

        return

    @attr(tags=["advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_03_migration_options_storage_tags(self):
        """ Test Volume migration options for Storage Pools with different Storage Tags
        """

        # Create PS-2 using Storage Tag
        storage_pool_2 = StoragePool.create(self.apiclient,
                                            self.services["nfs2"],
                                            clusterid=self.clusters[0].id,
                                            zoneid=self.zone.id,
                                            podid=self.pod.id,
                                            tags=self.services["storage_tags"]["a"]
                                            )
        self.cleanup.append(storage_pool_2)
        assert storage_pool_2.state == 'Up'
        storage_pools_response = list_storage_pools(self.apiclient,
                                                    id=storage_pool_2.id)
        assert isinstance(storage_pools_response, list) and len(storage_pools_response) > 0
        storage_response = storage_pools_response[0]
        assert storage_response.id == storage_pool_2.id and storage_response.type == storage_pool_2.type

        vm_1_volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='ROOT',
            listall=True
        )
        vol = vm_1_volumes[0]

        if self.hypervisor.lower() not in ["vmware", "xenserver"]:
            self.virtual_machine_1.stop(self.apiclient)

        volumePool = StoragePool.list(
            self.apiclient,
            id=vol.storageid
        )
        self.debug("Volume %s is on storage: %s" % (vol.id, volumePool))
        allStoragePools = StoragePool.list(
            self.apiclient
        )
        self.debug("All storage pools in the system: %s" % (allStoragePools))
        # Check migration options for volume
        pools_response = StoragePool.listForMigration(
            self.apiclient,
            id=vol.id
        )
        pools_suitable = [p for p in pools_response if p.suitableformigration]

        self.debug("Suitable storage pools found: %s" % len(pools_suitable))
        self.assertEqual(1, len(pools_suitable), "Check that there is only one item on the list")
        self.assertEqual(pools_suitable[0].id, storage_pool_2.id, "Check that PS-2 is the migration option for volume")

        # Update PS-2 Storage Tags
        StoragePool.update(
            self.apiclient,
            id=storage_pool_2.id,
            tags=self.services["storage_tags"]["b"]
        )

        # Check migration options for volume after updating PS-2 Storage Tags
        pools_response = StoragePool.listForMigration(
            self.apiclient,
            id=vol.id
        )
        pools_suitable = [p for p in pools_response if p.suitableformigration]

        self.debug("Suitable storage pools found: %s" % len(pools_suitable))
        self.assertEqual(0, len(pools_suitable), "Check that there is no migration option for volume")

        return
