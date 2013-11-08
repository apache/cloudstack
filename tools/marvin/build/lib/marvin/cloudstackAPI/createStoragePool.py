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


"""Creates a storage pool."""
from baseCmd import *
from baseResponse import *
class createStoragePoolCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the name for the storage pool"""
        """Required"""
        self.name = None
        """the URL of the storage pool"""
        """Required"""
        self.url = None
        """the Zone ID for the storage pool"""
        """Required"""
        self.zoneid = None
        """bytes CloudStack can provision from this storage pool"""
        self.capacitybytes = None
        """IOPS CloudStack can provision from this storage pool"""
        self.capacityiops = None
        """the cluster ID for the storage pool"""
        self.clusterid = None
        """the details for the storage pool"""
        self.details = []
        """hypervisor type of the hosts in zone that will be attached to this storage pool. KVM, VMware supported as of now."""
        self.hypervisor = None
        """whether the storage should be managed by CloudStack"""
        self.managed = None
        """the Pod ID for the storage pool"""
        self.podid = None
        """the storage provider name"""
        self.provider = None
        """the scope of the storage: cluster or zone"""
        self.scope = None
        """the tags for the storage pool"""
        self.tags = None
        self.required = ["name","url","zoneid",]

class createStoragePoolResponse (baseResponse):
    def __init__(self):
        """the ID of the storage pool"""
        self.id = None
        """IOPS CloudStack can provision from this storage pool"""
        self.capacityiops = None
        """the ID of the cluster for the storage pool"""
        self.clusterid = None
        """the name of the cluster for the storage pool"""
        self.clustername = None
        """the date and time the storage pool was created"""
        self.created = None
        """the host's currently allocated disk size"""
        self.disksizeallocated = None
        """the total disk size of the storage pool"""
        self.disksizetotal = None
        """the host's currently used disk size"""
        self.disksizeused = None
        """the hypervisor type of the storage pool"""
        self.hypervisor = None
        """the IP address of the storage pool"""
        self.ipaddress = None
        """the name of the storage pool"""
        self.name = None
        """the storage pool path"""
        self.path = None
        """the Pod ID of the storage pool"""
        self.podid = None
        """the Pod name of the storage pool"""
        self.podname = None
        """the scope of the storage pool"""
        self.scope = None
        """the state of the storage pool"""
        self.state = None
        """true if this pool is suitable to migrate a volume, false otherwise"""
        self.suitableformigration = None
        """the tags for the storage pool"""
        self.tags = None
        """the storage pool type"""
        self.type = None
        """the Zone ID of the storage pool"""
        self.zoneid = None
        """the Zone name of the storage pool"""
        self.zonename = None
        """the ID of the latest async job acting on this object"""
        self.jobid = None
        """the current status of the latest async job acting on this object"""
        self.jobstatus = None

