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


"""Lists security groups"""
from baseCmd import *
from baseResponse import *
class listSecurityGroupsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """list the security group by the id provided"""
        self.id = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list objects by project"""
        self.projectid = None
        """lists security groups by name"""
        self.securitygroupname = None
        """List resources by tags (key/value pairs)"""
        self.tags = []
        """lists security groups by virtual machine id"""
        self.virtualmachineid = None
        self.required = []

class listSecurityGroupsResponse (baseResponse):
    def __init__(self):
        """the ID of the security group"""
        self.id = None
        """the account owning the security group"""
        self.account = None
        """the description of the security group"""
        self.description = None
        """the domain name of the security group"""
        self.domain = None
        """the domain ID of the security group"""
        self.domainid = None
        """the name of the security group"""
        self.name = None
        """the project name of the group"""
        self.project = None
        """the project id of the group"""
        self.projectid = None
        """the list of egress rules associated with the security group"""
        self.egressrule = []
        """the list of ingress rules associated with the security group"""
        self.ingressrule = []
        """the list of resource tags associated with the rule"""
        self.tags = []
        """the ID of the latest async job acting on this object"""
        self.jobid = None
        """the current status of the latest async job acting on this object"""
        self.jobstatus = None

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

