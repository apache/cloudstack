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


"""Creates a service offering."""
from baseCmd import *
from baseResponse import *
class createServiceOfferingCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the CPU number of the service offering"""
        """Required"""
        self.cpunumber = None
        """the CPU speed of the service offering in MHz."""
        """Required"""
        self.cpuspeed = None
        """the display text of the service offering"""
        """Required"""
        self.displaytext = None
        """the total memory of the service offering in MB"""
        """Required"""
        self.memory = None
        """the name of the service offering"""
        """Required"""
        self.name = None
        """bytes read rate of the disk offering"""
        self.bytesreadrate = None
        """bytes write rate of the disk offering"""
        self.byteswriterate = None
        """The deployment planner heuristics used to deploy a VM of this offering. If null, value of global config vm.deployment.planner is used"""
        self.deploymentplanner = None
        """the ID of the containing domain, null for public offerings"""
        self.domainid = None
        """the host tag for this service offering."""
        self.hosttags = None
        """io requests read rate of the disk offering"""
        self.iopsreadrate = None
        """io requests write rate of the disk offering"""
        self.iopswriterate = None
        """is this a system vm offering"""
        self.issystem = None
        """true if the virtual machine needs to be volatile so that on every reboot of VM, original root disk is dettached then destroyed and a fresh root disk is created and attached to VM"""
        self.isvolatile = None
        """restrict the CPU usage to committed service offering"""
        self.limitcpuuse = None
        """data transfer rate in megabits per second allowed. Supported only for non-System offering and system offerings having "domainrouter" systemvmtype"""
        self.networkrate = None
        """the HA for the service offering"""
        self.offerha = None
        """details for planner, used to store specific parameters"""
        self.serviceofferingdetails = []
        """the storage type of the service offering. Values are local and shared."""
        self.storagetype = None
        """the system VM type. Possible types are "domainrouter", "consoleproxy" and "secondarystoragevm"."""
        self.systemvmtype = None
        """the tags for this service offering."""
        self.tags = None
        self.required = ["cpunumber","cpuspeed","displaytext","memory","name",]

class createServiceOfferingResponse (baseResponse):
    def __init__(self):
        """the id of the service offering"""
        self.id = None
        """the number of CPU"""
        self.cpunumber = None
        """the clock rate CPU speed in Mhz"""
        self.cpuspeed = None
        """the date this service offering was created"""
        self.created = None
        """is this a  default system vm offering"""
        self.defaultuse = None
        """deployment strategy used to deploy VM."""
        self.deploymentplanner = None
        """bytes read rate of the service offering"""
        self.diskBytesReadRate = None
        """bytes write rate of the service offering"""
        self.diskBytesWriteRate = None
        """io requests read rate of the service offering"""
        self.diskIopsReadRate = None
        """io requests write rate of the service offering"""
        self.diskIopsWriteRate = None
        """an alternate display text of the service offering."""
        self.displaytext = None
        """Domain name for the offering"""
        self.domain = None
        """the domain id of the service offering"""
        self.domainid = None
        """the host tag for the service offering"""
        self.hosttags = None
        """is this a system vm offering"""
        self.issystem = None
        """true if the vm needs to be volatile, i.e., on every reboot of vm from API root disk is discarded and creates a new root disk"""
        self.isvolatile = None
        """restrict the CPU usage to committed service offering"""
        self.limitcpuuse = None
        """the memory in MB"""
        self.memory = None
        """the name of the service offering"""
        self.name = None
        """data transfer rate in megabits per second allowed."""
        self.networkrate = None
        """the ha support in the service offering"""
        self.offerha = None
        """additional key/value details tied with this service offering"""
        self.serviceofferingdetails = None
        """the storage type for this service offering"""
        self.storagetype = None
        """is this a the systemvm type for system vm offering"""
        self.systemvmtype = None
        """the tags for the service offering"""
        self.tags = None
        """the list of resource tags associated with service offering. The resource tags are not used for Volume/VM placement on the specific host."""
        self.resourcetags = []

class resourcetags:
    def __init__(self):
        """"the account associated with the tag"""
        self.account = None
        """"customer associated with the tag"""
        self.customer = None
        """"the domain associated with the tag"""
        self.domain = None
        """"the ID of the domain associated with the tag"""
        self.domainid = None
        """"tag key name"""
        self.key = None
        """"the project name where tag belongs to"""
        self.project = None
        """"the project id the tag belongs to"""
        self.projectid = None
        """"id of the resource"""
        self.resourceid = None
        """"resource type"""
        self.resourcetype = None
        """"tag value"""
        self.value = None

