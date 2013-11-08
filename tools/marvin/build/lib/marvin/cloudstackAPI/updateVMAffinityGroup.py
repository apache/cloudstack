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


"""Updates the affinity/anti-affinity group associations of a virtual machine. The VM has to be stopped and restarted for the new properties to take effect."""
from baseCmd import *
from baseResponse import *
class updateVMAffinityGroupCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """The ID of the virtual machine"""
        """Required"""
        self.id = None
        """comma separated list of affinity groups id that are going to be applied to the virtual machine. Should be passed only when vm is created from a zone with Basic Network support. Mutually exclusive with securitygroupnames parameter"""
        self.affinitygroupids = []
        """comma separated list of affinity groups names that are going to be applied to the virtual machine. Should be passed only when vm is created from a zone with Basic Network support. Mutually exclusive with securitygroupids parameter"""
        self.affinitygroupnames = []
        self.required = ["id",]

class updateVMAffinityGroupResponse (baseResponse):
    def __init__(self):
        """the ID of the virtual machine"""
        self.id = None
        """the account associated with the virtual machine"""
        self.account = None
        """the number of cpu this virtual machine is running with"""
        self.cpunumber = None
        """the speed of each cpu"""
        self.cpuspeed = None
        """the amount of the vm's CPU currently used"""
        self.cpuused = None
        """the date when this virtual machine was created"""
        self.created = None
        """the read (io) of disk on the vm"""
        self.diskioread = None
        """the write (io) of disk on the vm"""
        self.diskiowrite = None
        """the read (bytes) of disk on the vm"""
        self.diskkbsread = None
        """the write (bytes) of disk on the vm"""
        self.diskkbswrite = None
        """user generated name. The name of the virtual machine is returned if no displayname exists."""
        self.displayname = None
        """an optional field whether to the display the vm to the end user or not."""
        self.displayvm = None
        """the name of the domain in which the virtual machine exists"""
        self.domain = None
        """the ID of the domain in which the virtual machine exists"""
        self.domainid = None
        """the virtual network for the service offering"""
        self.forvirtualnetwork = None
        """the group name of the virtual machine"""
        self.group = None
        """the group ID of the virtual machine"""
        self.groupid = None
        """Os type ID of the virtual machine"""
        self.guestosid = None
        """true if high-availability is enabled, false otherwise"""
        self.haenable = None
        """the ID of the host for the virtual machine"""
        self.hostid = None
        """the name of the host for the virtual machine"""
        self.hostname = None
        """the hypervisor on which the template runs"""
        self.hypervisor = None
        """instance name of the user vm; this parameter is returned to the ROOT admin only"""
        self.instancename = None
        """true if vm contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory."""
        self.isdynamicallyscalable = None
        """an alternate display text of the ISO attached to the virtual machine"""
        self.isodisplaytext = None
        """the ID of the ISO attached to the virtual machine"""
        self.isoid = None
        """the name of the ISO attached to the virtual machine"""
        self.isoname = None
        """ssh key-pair"""
        self.keypair = None
        """the memory allocated for the virtual machine"""
        self.memory = None
        """the name of the virtual machine"""
        self.name = None
        """the incoming network traffic on the vm"""
        self.networkkbsread = None
        """the outgoing network traffic on the host"""
        self.networkkbswrite = None
        """the password (if exists) of the virtual machine"""
        self.password = None
        """true if the password rest feature is enabled, false otherwise"""
        self.passwordenabled = None
        """the project name of the vm"""
        self.project = None
        """the project id of the vm"""
        self.projectid = None
        """public IP address id associated with vm via Static nat rule"""
        self.publicip = None
        """public IP address id associated with vm via Static nat rule"""
        self.publicipid = None
        """device ID of the root volume"""
        self.rootdeviceid = None
        """device type of the root volume"""
        self.rootdevicetype = None
        """the ID of the service offering of the virtual machine"""
        self.serviceofferingid = None
        """the name of the service offering of the virtual machine"""
        self.serviceofferingname = None
        """State of the Service from LB rule"""
        self.servicestate = None
        """the state of the virtual machine"""
        self.state = None
        """an alternate display text of the template for the virtual machine"""
        self.templatedisplaytext = None
        """the ID of the template for the virtual machine. A -1 is returned if the virtual machine was created from an ISO file."""
        self.templateid = None
        """the name of the template for the virtual machine"""
        self.templatename = None
        """the ID of the availablility zone for the virtual machine"""
        self.zoneid = None
        """the name of the availability zone for the virtual machine"""
        self.zonename = None
        """list of affinity groups associated with the virtual machine"""
        self.affinitygroup = []
        """the list of nics associated with vm"""
        self.nic = []
        """list of security groups associated with the virtual machine"""
        self.securitygroup = []
        """the list of resource tags associated with vm"""
        self.tags = []
        """the ID of the latest async job acting on this object"""
        self.jobid = None
        """the current status of the latest async job acting on this object"""
        self.jobstatus = None

class affinitygroup:
    def __init__(self):
        """"the ID of the affinity group"""
        self.id = None
        """"the account owning the affinity group"""
        self.account = None
        """"the description of the affinity group"""
        self.description = None
        """"the domain name of the affinity group"""
        self.domain = None
        """"the domain ID of the affinity group"""
        self.domainid = None
        """"the name of the affinity group"""
        self.name = None
        """"the type of the affinity group"""
        self.type = None
        """"virtual machine Ids associated with this affinity group"""
        self.virtualmachineIds = None

class nic:
    def __init__(self):
        """"the ID of the nic"""
        self.id = None
        """"the broadcast uri of the nic"""
        self.broadcasturi = None
        """"the gateway of the nic"""
        self.gateway = None
        """"the IPv6 address of network"""
        self.ip6address = None
        """"the cidr of IPv6 network"""
        self.ip6cidr = None
        """"the gateway of IPv6 network"""
        self.ip6gateway = None
        """"the ip address of the nic"""
        self.ipaddress = None
        """"true if nic is default, false otherwise"""
        self.isdefault = None
        """"the isolation uri of the nic"""
        self.isolationuri = None
        """"true if nic is default, false otherwise"""
        self.macaddress = None
        """"the netmask of the nic"""
        self.netmask = None
        """"the ID of the corresponding network"""
        self.networkid = None
        """"the name of the corresponding network"""
        self.networkname = None
        """"the Secondary ipv4 addr of nic"""
        self.secondaryip = None
        """"the traffic type of the nic"""
        self.traffictype = None
        """"the type of the nic"""
        self.type = None

class egressrule:
    def __init__(self):
        """"account owning the security group rule"""
        self.account = None
        """"the CIDR notation for the base IP address of the security group rule"""
        self.cidr = None
        """"the ending IP of the security group rule"""
        self.endport = None
        """"the code for the ICMP message response"""
        self.icmpcode = None
        """"the type of the ICMP message response"""
        self.icmptype = None
        """"the protocol of the security group rule"""
        self.protocol = None
        """"the id of the security group rule"""
        self.ruleid = None
        """"security group name"""
        self.securitygroupname = None
        """"the starting IP of the security group rule"""
        self.startport = None

class ingressrule:
    def __init__(self):
        """"account owning the security group rule"""
        self.account = None
        """"the CIDR notation for the base IP address of the security group rule"""
        self.cidr = None
        """"the ending IP of the security group rule"""
        self.endport = None
        """"the code for the ICMP message response"""
        self.icmpcode = None
        """"the type of the ICMP message response"""
        self.icmptype = None
        """"the protocol of the security group rule"""
        self.protocol = None
        """"the id of the security group rule"""
        self.ruleid = None
        """"security group name"""
        self.securitygroupname = None
        """"the starting IP of the security group rule"""
        self.startport = None

class tags:
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

class securitygroup:
    def __init__(self):
        """"the ID of the security group"""
        self.id = None
        """"the account owning the security group"""
        self.account = None
        """"the description of the security group"""
        self.description = None
        """"the domain name of the security group"""
        self.domain = None
        """"the domain ID of the security group"""
        self.domainid = None
        """"the name of the security group"""
        self.name = None
        """"the project name of the group"""
        self.project = None
        """"the project id of the group"""
        self.projectid = None
        """"the list of egress rules associated with the security group"""
        self.egressrule = []
        """"account owning the security group rule"""
        self.account = None
        """"the CIDR notation for the base IP address of the security group rule"""
        self.cidr = None
        """"the ending IP of the security group rule"""
        self.endport = None
        """"the code for the ICMP message response"""
        self.icmpcode = None
        """"the type of the ICMP message response"""
        self.icmptype = None
        """"the protocol of the security group rule"""
        self.protocol = None
        """"the id of the security group rule"""
        self.ruleid = None
        """"security group name"""
        self.securitygroupname = None
        """"the starting IP of the security group rule"""
        self.startport = None
        """"the list of ingress rules associated with the security group"""
        self.ingressrule = []
        """"account owning the security group rule"""
        self.account = None
        """"the CIDR notation for the base IP address of the security group rule"""
        self.cidr = None
        """"the ending IP of the security group rule"""
        self.endport = None
        """"the code for the ICMP message response"""
        self.icmpcode = None
        """"the type of the ICMP message response"""
        self.icmptype = None
        """"the protocol of the security group rule"""
        self.protocol = None
        """"the id of the security group rule"""
        self.ruleid = None
        """"security group name"""
        self.securitygroupname = None
        """"the starting IP of the security group rule"""
        self.startport = None
        """"the list of resource tags associated with the rule"""
        self.tags = []
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
        """"the ID of the latest async job acting on this object"""
        self.jobid = None
        """"the current status of the latest async job acting on this object"""
        self.jobstatus = None

class tags:
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

