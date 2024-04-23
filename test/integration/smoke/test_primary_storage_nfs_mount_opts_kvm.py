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

import marvin
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

    def setUp(self):
        self.testClient = super(TestNFSMountOptsKVM, self).getClsTestClient()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.logger = logging.getLogger('TestHAKVM')

        self.cluster = list_clusters(self.apiclient)[0]
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.host = self.getHost()
        self.storagePool = self.getPrimaryStorage(self.cluster.id)
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        self.cluster_id = self.host.clusterid
        self.hostConfig["password"]="P@ssword123"
        self.sshClient = SshClient(
            host=self.host.ipaddress,
            port=22,
            user=self.hostConfig['username'],
            passwd=self.hostConfig['password'])


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
            self.storage_pool = response[0]
            return self.storage_pool
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

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="true")
    def test_primary_storage_nfs_options_kvm(self):
        """
            Tests that NFS mount options configured on the primary storage are set correctly on the KVM hypervisor host
        """

        vers = self.getNFSMountOptionForPool("vers", self.storage_pool.id)
        if (vers == None):
            raise self.skipTest("Storage pool not associated with the host")

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

        maint_cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        maint_cmd.id = self.storage_pool.id
        resp = self.apiclient.enableStorageMaintenance(maint_cmd)

        storage = StoragePool.update(self.apiclient,
                                     id=self.storage_pool.id,
                                     details=details
                                     )

        store_maint_cmd = cancelStorageMaintenance.cancelStorageMaintenanceCmd()
        store_maint_cmd.id = self.storage_pool.id
        resp = self.apiclient.cancelStorageMaintenance(store_maint_cmd)

        storage = StoragePool.list(self.apiclient,
                                   id=self.storage_pool.id
                                   )[0]

        self.assertEqual(storage.nfsmountopts, nfsMountOpts)

        options = self.getNFSMountOptionsFromVirsh(self.storage_pool.id)

        self.assertEqual(options["vers"], version)
        if (nconnect != None):
            self.assertEqual(options["nconnect"], nconnect)
