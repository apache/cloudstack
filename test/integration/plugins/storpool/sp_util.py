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
from marvin.codes import FAILED, KVM, PASS, XEN_SERVER, RUNNING
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import random_gen, cleanup_resources, validateList, is_snapshot_on_nfs, isAlmostEqual
from marvin.lib.base import (Account,
                             Cluster,
                             Configurations,
                             ServiceOffering,
                             Snapshot,
                             StoragePool,
                             Template,
                             VirtualMachine,
                             VmSnapshot,
                             Volume)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_disk_offering,
                               list_hosts,
                               list_snapshots,
                               list_storage_pools,
                               list_volumes,
                               list_virtual_machines,
                               list_configurations,
                               list_service_offering,
                               list_clusters,
                               list_zones)
from marvin.cloudstackAPI import (listOsTypes,
                                  listTemplates,
                                  listHosts,
                                  listClusters,
                                  createTemplate,
                                  createVolume,
                                  getVolumeSnapshotDetails,
                                  resizeVolume,
                                  authorizeSecurityGroupIngress,
                                  migrateVirtualMachineWithVolume,
                                  destroyVirtualMachine,
                                  deployVirtualMachine,
                                  createAccount,
                                  startVirtualMachine,
                                  )
import time
import pprint
import random
from marvin.configGenerator import configuration
import uuid
import logging
import subprocess
import json
from storpool import spapi
from storpool import sptypes

class TestData():
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    diskName = "diskname"
    diskOffering = "diskoffering"
    diskOffering2 = "diskoffering2"
    cephDiskOffering = "cephDiskOffering"
    nfsDiskOffering = "nfsDiskOffering"
    domainId = "domainId"
    hypervisor = "hypervisor"
    login = "login"
    mvip = "mvip"
    password = "password"
    port = "port"
    primaryStorage = "primarystorage"
    primaryStorage2 = "primarystorage2"
    primaryStorage3 = "primarystorage3"
    primaryStorage4 = "primaryStorage4"
    provider = "provider"
    serviceOffering = "serviceOffering"
    serviceOfferingssd2 = "serviceOffering-ssd2"
    serviceOfferingsPrimary = "serviceOfferingsPrimary"
    serviceOfferingsIops = "serviceOfferingsIops"
    serviceOfferingsCeph = "serviceOfferingsCeph"
    scope = "scope"
    StorPool = "StorPool"
    storageTag = ["ssd", "ssd2"]
    tags = "tags"
    virtualMachine = "virtualmachine"
    virtualMachine2 = "virtualmachine2"
    volume_1 = "volume_1"
    volume_2 = "volume_2"
    volume_3 = "volume_3"
    volume_4 = "volume_4"
    volume_5 = "volume_5"
    volume_6 = "volume_6"
    volume_7 = "volume_7"
    zoneId = "zoneId"

    def __init__(self):
        sp_template_1 = 'ssd'
        sp_template_2 = 'ssd2'
        sp_template_3 = 'test-primary'
        self.testdata = {
            TestData.primaryStorage: {
                "name": sp_template_1,
                TestData.scope: "ZONE",
                "url": sp_template_1,
                TestData.provider: "StorPool",
                "path": "/dev/storpool",
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "KVM"
            },
            TestData.primaryStorage2: {
                "name": sp_template_2,
                TestData.scope: "ZONE",
                "url": sp_template_2,
                TestData.provider: "StorPool",
                "path": "/dev/storpool",
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "KVM"
            },
            TestData.primaryStorage3: {
                "name": sp_template_3,
                TestData.scope: "ZONE",
                "url": sp_template_3,
                TestData.provider: "StorPool",
                "path": "/dev/storpool",
                TestData.capacityBytes: 2251799813685248,
                TestData.hypervisor: "KVM"
            },
            TestData.primaryStorage4: {
                "name": "ceph",
                TestData.scope: "ZONE",
                TestData.provider: "RBD",
                TestData.hypervisor: "KVM"
            },
            TestData.virtualMachine: {
                "name": "TestVM",
                "displayname": "TestVM",
                "privateport": 22,
                "publicport": 22,
                "protocol": "tcp"
            },
            TestData.virtualMachine2: {
                "name": "TestVM2",
                "displayname": "TestVM2",
                "privateport": 22,
                "publicport": 22,
                "protocol": "tcp"
            },
            TestData.serviceOffering:{
                "name": sp_template_1,
                "displaytext": "SP_CO_2 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
                "hypervisorsnapshotreserve": 200,
                "tags": sp_template_1
            },
            TestData.serviceOfferingssd2:{
                "name": sp_template_2,
                "displaytext": "SP_CO_2 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
                "hypervisorsnapshotreserve": 200,
                "tags": sp_template_2
            },
            TestData.serviceOfferingsPrimary:{
                "name": "nfs",
                "displaytext": "SP_CO_2 (Min IOPS = 10,000; Max IOPS = 15,000)",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
                "hypervisorsnapshotreserve": 200,
                "tags": "nfs"
            },
            TestData.serviceOfferingsCeph:{
                "name": "ceph",
                "displaytext": "Ceph Service offerings",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": False,
                "hypervisorsnapshotreserve": 200,
                "tags": "ceph"
            },
            TestData.serviceOfferingsIops:{
                "name": "iops",
                "displaytext": "Testing IOPS on StorPool",
                "cpunumber": 1,
                "cpuspeed": 500,
                "memory": 512,
                "storagetype": "shared",
                "customizediops": True,
                "tags": sp_template_1,
            },
            TestData.diskOffering: {
                "name": "SP_DO_1",
                "displaytext": "SP_DO_1 (5GB Min IOPS = 300; Max IOPS = 500)",
                "disksize": 5,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: sp_template_1,
                "storagetype": "shared"
            },
            TestData.diskOffering2: {
                "name": "SP_DO_1",
                "displaytext": "SP_DO_1 (5GB Min IOPS = 300; Max IOPS = 500)",
                "disksize": 5,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: sp_template_2,
                "storagetype": "shared"
            },
            TestData.cephDiskOffering: {
                "name": "ceph",
                "displaytext": "Ceph fixed disk offering",
                "disksize": 5,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: "ceph",
                "storagetype": "shared"
            },
            TestData.nfsDiskOffering: {
                "name": "nfs",
                "displaytext": "NFS fixed disk offering",
                "disksize": 5,
                "customizediops": False,
                "miniops": 300,
                "maxiops": 500,
                "hypervisorsnapshotreserve": 200,
                TestData.tags: "nfs",
                "storagetype": "shared"
            },
            TestData.volume_1: {
                TestData.diskName: "test-volume-1",
            },
            TestData.volume_2: {
                TestData.diskName: "test-volume-2",
            },
            TestData.volume_3: {
                TestData.diskName: "test-volume-3",
            },
            TestData.volume_4: {
                TestData.diskName: "test-volume-4",
            },
            TestData.volume_5: {
                TestData.diskName: "test-volume-5",
            },
            TestData.volume_6: {
                TestData.diskName: "test-volume-6",
            },
            TestData.volume_7: {
                TestData.diskName: "test-volume-7",
            },
        }
class StorPoolHelper():

    @classmethod
    def create_template_from_snapshot(self, apiclient, services, snapshotid=None, volumeid=None):
        """Create template from Volume"""
        # Create template from Virtual machine and Volume ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = "StorPool_Template"
        cmd.name = "-".join(["StorPool-", random_gen()])
        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            raise Exception(
                "Unable to find Ostype is required for creating template")

        cmd.isfeatured = True
        cmd.ispublic = True
        cmd.isextractable =  False

        if snapshotid:
            cmd.snapshotid = snapshotid
        if volumeid:
            cmd.volumeid = volumeid
        return Template(apiclient.createTemplate(cmd).__dict__)

    @classmethod
    def getCfgFromUrl(self, url):
        cfg = dict([
            option.split('=')
            for option in url.split(';')
        ])
        host, port = cfg['SP_API_HTTP'].split(':')
        auth = cfg['SP_AUTH_TOKEN']
        return host, int(port), auth

    @classmethod
    def get_remote_storpool_cluster(cls):
       logging.debug("######################## get_remote_storpool_cluster")

       storpool_clusterid = subprocess.check_output(['storpool_confshow', 'CLUSTER_ID']).strip()
       clusterid = storpool_clusterid.split("=")[1].split(".")[1]
       logging.debug("######################## %s"  % storpool_clusterid)
       cmd = ["storpool", "-j", "cluster", "list"]
       proc = subprocess.Popen(cmd,stdout=subprocess.PIPE).stdout.read()
       csl = json.loads(proc)
       logging.debug("######################## %s"  % csl)

       clusters =  csl.get("data").get("clusters")
       logging.debug("######################## %s"  % clusters)

       for c in clusters:
           c_id = c.get("id")
           if c_id != clusterid:
               return c.get("name")

    @classmethod
    def get_local_cluster(cls, apiclient, zoneid):
       storpool_clusterid = subprocess.check_output(['storpool_confshow', 'CLUSTER_ID'])
       clusterid = storpool_clusterid.split("=")
       logging.debug(storpool_clusterid)
       clusters = list_clusters(apiclient, zoneid = zoneid)
       for c in clusters:
           configuration = list_configurations(
               apiclient,
               clusterid = c.id
               )
           for conf in configuration:
               if conf.name == 'sp.cluster.id'  and (conf.value in clusterid[1]):
                   return c

    @classmethod
    def get_remote_cluster(cls, apiclient, zoneid):
       storpool_clusterid = subprocess.check_output(['storpool_confshow', 'CLUSTER_ID'])
       clusterid = storpool_clusterid.split("=")
       logging.debug(storpool_clusterid)
       clusters = list_clusters(apiclient, zoneid = zoneid)
       for c in clusters:
           configuration = list_configurations(
               apiclient,
               clusterid = c.id
               )
           for conf in configuration:
               if conf.name == 'sp.cluster.id'  and (conf.value not in clusterid[1]):
                   return c

    @classmethod
    def get_snapshot_template_id(self, apiclient, snapshot, storage_pool_id):
        try:
            cmd = getVolumeSnapshotDetails.getVolumeSnapshotDetailsCmd()
            cmd.snapshotid = snapshot.id
            snapshot_details = apiclient.getVolumeSnapshotDetails(cmd)
            logging.debug("Snapshot details %s" % snapshot_details)
            logging.debug("Snapshot with uuid %s" % snapshot.id)
            for s in snapshot_details:
                if s["snapshotDetailsName"] == storage_pool_id:
                    return s["snapshotDetailsValue"]
        except Exception as err:
           raise Exception(err)

        return None

    @classmethod
    def getDestinationHost(self, hostsToavoid, hosts):
        destinationHost = None
        for host in hosts:
            if host.id not in hostsToavoid:
                destinationHost = host
                break
        return destinationHost


    @classmethod
    def getDestinationPool(self,
                           poolsToavoid,
                           migrateto,
                           pools
                           ):
        """ Get destination pool which has scope same as migrateto
        and which is not in avoid set
        """

        destinationPool = None

        # Get Storage Pool Id to migrate to
        for storagePool in pools:
            if storagePool.scope == migrateto:
                if storagePool.name not in poolsToavoid:
                    destinationPool = storagePool
                    break

        return destinationPool

    @classmethod
    def get_destination_pools_hosts(self, apiclient, vm, hosts):
        vol_list = list_volumes(
            apiclient,
            virtualmachineid=vm.id,
            listall=True)
            # Get destination host
        destinationHost = self.getDestinationHost(vm.hostid, hosts)
        return destinationHost, vol_list

    @classmethod
    def list_hosts_by_cluster_id(cls, apiclient, clusterid):
        """List all Hosts matching criteria"""
        cmd = listHosts.listHostsCmd()
        cmd.clusterid = clusterid
        return(apiclient.listHosts(cmd))

    @classmethod
    def set_securityGroups(cls, apiclient, account, domainid, id):
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = '0.0.0.0/0'
        cmd.securitygroupid = id
        cmd.account = account
        cmd.domainid = domainid

        apiclient.authorizeSecurityGroupIngress(cmd)

        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        # Authorize to only account not CIDR
        cmd.securitygroupid = id
        cmd.account = account
        cmd.domainid = domainid
        apiclient.authorizeSecurityGroupIngress(cmd)

    @classmethod
    def migrateVm(self, apiclient, vm, destinationHost):
        """
        This method is to migrate a VM using migrate virtual machine API
        """

        vm.migrate(
            apiclient,
            hostid=destinationHost.id,
        )
        vm.getState(
            apiclient,
            "Running"
        )
        # check for the VM's host and volume's storage post migration
        migrated_vm_response = list_virtual_machines(apiclient, id=vm.id)
        assert isinstance(migrated_vm_response, list), "Check list virtual machines response for valid list"

        assert migrated_vm_response[0].hostid ==  destinationHost.id, "VM did not migrate to a specified host"
        return migrated_vm_response[0]

    @classmethod
    def migrateVmWithVolumes(self, apiclient, vm, destinationHost, volumes, pool):
        """
            This method is used to migrate a vm and its volumes using migrate virtual machine with volume API
            INPUTS:
                   1. vm -> virtual machine object
                   2. destinationHost -> the host to which VM will be migrated
                   3. volumes -> list of volumes which are to be migrated
                   4. pools -> list of destination pools
        """
        vol_pool_map = {vol.id: pool.id for vol in volumes}

        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()
        cmd.hostid = destinationHost.id
        cmd.migrateto = []
        cmd.virtualmachineid = self.virtual_machine.id
        for volume, pool1 in vol_pool_map.items():
            cmd.migrateto.append({
                'volume': volume,
                'pool': pool1
        })
        apiclient.migrateVirtualMachineWithVolume(cmd)

        vm.getState(
            apiclient,
            "Running"
        )
        # check for the VM's host and volume's storage post migration
        migrated_vm_response = list_virtual_machines(apiclient, id=vm.id)
        assert isinstance(migrated_vm_response, list), "Check list virtual machines response for valid list"

        assert migrated_vm_response[0].hostid == destinationHost.id, "VM did not migrate to a specified host"

        for vol in volumes:
            migrated_volume_response = list_volumes(
                apiclient,
                virtualmachineid=migrated_vm_response[0].id,
                name=vol.name,
                listall=True)
            assert isinstance(migrated_volume_response, list), "Check list virtual machines response for valid list"
            assert migrated_volume_response[0].storageid == pool.id, "Volume did not migrate to a specified pool"

            assert str(migrated_volume_response[0].state).lower().eq('ready'), "Check migrated volume is in Ready state"

            return migrated_vm_response[0]

    @classmethod
    def create_sp_template_and_storage_pool(self, apiclient, template_name, primary_storage, zoneid):
        spapiRemote = spapi.Api.fromConfig()
        logging.debug("================ %s" % spapiRemote)

        sp_api = spapi.Api.fromConfig(multiCluster= True)
        logging.debug("================ %s" % sp_api)

        remote_cluster = self.get_remote_storpool_cluster()
        logging.debug("================ %s" % remote_cluster)

        newTemplate = sptypes.VolumeTemplateCreateDesc(name = template_name, placeAll = "ssd", placeTail = "ssd", placeHead = "ssd", replication=1)
        template_on_remote = spapiRemote.volumeTemplateCreate(newTemplate, clusterName = remote_cluster)
        template_on_local = spapiRemote.volumeTemplateCreate(newTemplate)
        storage_pool = StoragePool.create(apiclient, primary_storage, zoneid = zoneid,)
        return storage_pool, spapiRemote, sp_api

    @classmethod
    def destroy_vm(self, apiclient, virtualmachineid):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = virtualmachineid
        cmd.expunge = True
        apiclient.destroyVirtualMachine(cmd)

    @classmethod
    def check_storpool_volume_size(cls, volume, spapi):
        name = volume.path.split("/")[3]
        try:
            spvolume = spapi.volumeList(volumeName = "~" + name)
            if spvolume[0].size != volume.size:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

    @classmethod
    def check_storpool_volume_iops(cls, spapi, volume,):
        name = volume.path.split("/")[3]
        try:
            spvolume = spapi.volumeList(volumeName = "~" + name)
            logging.debug(spvolume[0].iops)
            logging.debug(volume.maxiops)
            if spvolume[0].iops != volume.maxiops:
                raise Exception("Storpool volume size is not the same as CloudStack db size")
        except spapi.ApiError as err:
           raise Exception(err)

    @classmethod
    def create_custom_disk(cls, apiclient, services, size = None, miniops = None, maxiops =None, diskofferingid=None, zoneid=None, account=None, domainid=None, snapshotid=None):
        """Create Volume from Custom disk offering"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]

        if diskofferingid:
            cmd.diskofferingid = diskofferingid

        if size:
            cmd.size = size

        if miniops:
            cmd.miniops = miniops

        if maxiops:
            cmd.maxiops = maxiops

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid

        if snapshotid:
            cmd.snapshotid = snapshotid

        cmd.zoneid = zoneid

        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_vm_custom(cls, apiclient, services, templateid=None, zoneid=None,
               serviceofferingid=None, method='GET', hypervisor=None,
               cpuNumber=None, cpuSpeed=None, memory=None, minIops=None,
               maxIops=None, hostid=None, rootdisksize=None, account=None, domainid=None
               ):
        """Create the instance"""

        cmd = deployVirtualMachine.deployVirtualMachineCmd()

        if serviceofferingid:
            cmd.serviceofferingid = serviceofferingid
        elif "serviceoffering" in services:
            cmd.serviceofferingid = services["serviceoffering"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if hypervisor:
            cmd.hypervisor = hypervisor

        if hostid:
            cmd.hostid = hostid

        if "displayname" in services:
            cmd.displayname = services["displayname"]

        if "name" in services:
            cmd.name = services["name"]

        if templateid:
            cmd.templateid = templateid
        elif "template" in services:
            cmd.templateid = services["template"]

        cmd.details = [{}]

        if cpuNumber:
            cmd.details[0]["cpuNumber"] = cpuNumber

        if cpuSpeed:
            cmd.details[0]["cpuSpeed"] = cpuSpeed

        if memory:
            cmd.details[0]["memory"] = memory

        if minIops:
            cmd.details[0]["minIops"] = minIops

        if maxIops:
            cmd.details[0]["maxIops"] = maxIops

        if rootdisksize >= 0:
            cmd.details[0]["rootdisksize"] = rootdisksize

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid

        virtual_machine = apiclient.deployVirtualMachine(cmd, method=method)

        return VirtualMachine(virtual_machine.__dict__, services)


    @classmethod
    def resize_volume(cls, apiclient, volume, shrinkOk=None, disk_offering =None, size=None, maxiops=None, miniops=None):

        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = volume.id

        if disk_offering:
            cmd.diskofferingid = disk_offering.id
        if size:
            cmd.size = size

        if maxiops:
            cmd.maxiops = maxiops
        if miniops:
            cmd.miniops

        cmd.shrinkok = shrinkOk
        apiclient.resizeVolume(cmd)

        new_size = Volume.list(
            apiclient,
            id=volume.id
            )
        volume_size = new_size[0].size
        return new_size[0]

    @classmethod
    def create_account(cls, apiclient, services, accounttype=None, domainid=None, roleid=None):
        """Creates an account"""
        cmd = createAccount.createAccountCmd()

        # 0 - User, 1 - Root Admin, 2 - Domain Admin
        if accounttype:
            cmd.accounttype = accounttype
        else:
            cmd.accounttype = 1

        cmd.email = services["email"]
        cmd.firstname = services["firstname"]
        cmd.lastname = services["lastname"]

        cmd.password = services["password"]
        username = services["username"]
        # Limit account username to 99 chars to avoid failure
        # 6 chars start string + 85 chars apiclientid + 6 chars random string + 2 chars joining hyphen string = 99
        username = username[:6]
        apiclientid = apiclient.id[-85:] if len(apiclient.id) > 85 else apiclient.id
        cmd.username = "-".join([username,
                             random_gen(id=apiclientid, size=6)])

        if "accountUUID" in services:
            cmd.accountid = "-".join([services["accountUUID"], random_gen()])

        if "userUUID" in services:
            cmd.userid = "-".join([services["userUUID"], random_gen()])

        if domainid:
            cmd.domainid = domainid

        if roleid:
            cmd.roleid = roleid

        account = apiclient.createAccount(cmd)

        return Account(account.__dict__)

    def start(cls, apiclient, vmid, hostid):
        """Start the instance"""
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = vmid
        cmd.hostid = hostid
        return (apiclient.startVirtualMachine(cmd))

    @classmethod
    def getClustersWithStorPool(cls, apiclient, zoneId,):
        cmd = listClusters.listClustersCmd()
        cmd.zoneid = zoneId
        cmd.allocationstate = "Enabled"
        clusters = apiclient.listClusters(cmd)
        clustersToDeploy = []
        for cluster in clusters:
            if cluster.resourcedetails['sp.cluster.id']:
                clustersToDeploy.append(cluster.id)

        return clustersToDeploy

    @classmethod
    def getHostToDeployOrMigrate(cls, apiclient, hostsToavoid, clustersToDeploy):
        hostsOnCluster = []
        for c in clustersToDeploy:
            hostsOnCluster.append(cls.list_hosts_by_cluster_id(apiclient, c))

        destinationHost = None
        for host in hostsOnCluster:

            if hostsToavoid is None:
                return host[0]
            if host[0].id not in hostsToavoid:
                destinationHost = host[0]
                break

        return destinationHost
