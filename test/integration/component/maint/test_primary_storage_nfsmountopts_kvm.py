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

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import (list_clusters, list_hosts, list_storage_pools)
import xml.etree.ElementTree as ET
from lxml import etree
from nose.plugins.attrib import attr

class TestNFSMountOptsKVM(cloudstackTestCase):
    """ Test cases for host HA using KVM host(s)
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestNFSMountOptsKVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()

        cls.cluster = list_clusters(cls.apiclient)[0]
        cls.host = cls.getHost(cls)
        cls.storage_pool = cls.getPrimaryStorage(cls, cls.cluster.id)
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cls.cluster_id = cls.host.clusterid
        cls.sshClient = SshClient(
            host=cls.host.ipaddress,
            port=22,
            user=cls.hostConfig['username'],
            passwd=cls.hostConfig['password'])
        cls.version = cls.getNFSMountOptionForPool(cls, "vers", cls.storage_pool.id)
        if (cls.version == None):
            raise cls.skipTest("Storage pool not associated with the host")

    def tearDown(self):
        nfsMountOpts = "vers=" + self.version
        details = [{'nfsmountopts': nfsMountOpts}]
        self.changeNFSOptions(details)
        pass

    def getHost(self):
        response = list_hosts(
            self.apiclient,
            type='Routing',
            hypervisor='kvm',
            state='Up',
            id=None
        )

        if response and len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("Not enough KVM hosts found, skipping NFS options test")

    def getPrimaryStorage(self, clusterId):
        response = list_storage_pools(
            self.apiclient,
            clusterid=clusterId,
            type='NetworkFilesystem',
            state='Up',
            id=None,
        )
        if response and len(response) > 0:
            return response[0]
        raise self.skipTest("Not enough KVM hosts found, skipping NFS options test")

    def getNFSMountOptionsFromVirsh(self, poolId):
        virsh_cmd = "virsh pool-dumpxml %s" % poolId
        xml_res = self.sshClient.execute(virsh_cmd)
        xml_as_str = ''.join(xml_res)
        self.debug(xml_as_str)
        parser = etree.XMLParser(remove_blank_text=True)
        root = ET.fromstring(xml_as_str, parser=parser)
        mount_opts = root.findall("{http://libvirt.org/schemas/storagepool/fs/1.0}mount_opts")[0]

        options_map = {}
        for child in mount_opts:
            option = child.get('name').split("=")
            options_map[option[0]] = option[1]
        return options_map

    def getUnusedNFSVersions(self, filter):
        nfsstat_cmd = "nfsstat -m | sed -n '/%s/{ n; p }'" % filter
        nfsstats = self.sshClient.execute(nfsstat_cmd)
        versions = {"4.1": 0, "4.2": 0, "3": 0}
        for stat in nfsstats:
            vers = stat[stat.find("vers"):].split("=")[1].split(",")[0]
            versions[vers] += 1

        for key in versions:
            if versions[key] == 0:
                return key
        return None

    def getNFSMountOptionForPool(self, option, poolId):
        nfsstat_cmd = "nfsstat -m | sed -n '/%s/{ n; p }'" % poolId
        nfsstat = self.sshClient.execute(nfsstat_cmd)
        if (nfsstat == None):
            return None
        stat = nfsstat[0]
        vers = stat[stat.find(option):].split("=")[1].split(",")[0]
        return vers

    def changeNFSOptions(self, details):
        maint_cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        maint_cmd.id = self.storage_pool.id

        storage = StoragePool.list(self.apiclient,
                                   id=self.storage_pool.id
                                   )[0]
        if storage.state != "Maintenance":
            self.apiclient.enableStorageMaintenance(maint_cmd)

        StoragePool.update(self.apiclient,
                           id=self.storage_pool.id,
                           details=details
                           )

        store_maint_cmd = cancelStorageMaintenance.cancelStorageMaintenanceCmd()
        store_maint_cmd.id = self.storage_pool.id
        resp = self.apiclient.cancelStorageMaintenance(store_maint_cmd)
        return resp

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_primary_storage_nfs_options_kvm(self):
        """
            Tests that NFS mount options configured on the primary storage are set correctly on the KVM hypervisor host
        """
        version = self.getUnusedNFSVersions(self.storage_pool.ipaddress)
        nconnect = None
        if version == None:
            self.debug("skipping nconnect mount option as there are multiple mounts already present from the nfs server for all versions")
            version = self.getUnusedNFSVersions(self.storage_pool.id)
            nfsMountOpts = "vers=" + version
        else:
            nconnect='4'
            nfsMountOpts = "vers=" + version + ",nconnect=" + nconnect

        details = [{'nfsmountopts': nfsMountOpts}]

        resp = self.changeNFSOptions(details)

        storage = StoragePool.list(self.apiclient,
                                   id=self.storage_pool.id
                                   )[0]
        self.assertEqual(storage.nfsmountopts, nfsMountOpts)

        options = self.getNFSMountOptionsFromVirsh(self.storage_pool.id)
        self.assertEqual(options["vers"], version)
        if (nconnect != None):
            self.assertEqual(options["nconnect"], nconnect)

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_primary_storage_incorrect_nfs_options_kvm(self):
        """
            Tests that incorrect NFS mount options leads to exception when maintenance mode is cancelled
        """
        nfsMountOpts = "version=4.1"
        details = [{'nfsmountopts': nfsMountOpts}]

        try:
            resp = self.changeNFSOptions(details)
        except Exception:
            storage = StoragePool.list(self.apiclient,
                                       id=self.storage_pool.id
                                       )[0]
            self.assertEqual(storage.state, "Maintenance")
        else:
            self.fail("Incorrect NFS mount option should throw error while mounting")
