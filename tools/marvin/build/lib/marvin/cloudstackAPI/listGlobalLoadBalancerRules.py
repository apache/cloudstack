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


"""Lists load balancer rules."""
from baseCmd import *
from baseResponse import *
class listGlobalLoadBalancerRulesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """the ID of the global load balancer rule"""
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
        """region ID"""
        self.regionid = None
        """List resources by tags (key/value pairs)"""
        self.tags = []
        self.required = []

class listGlobalLoadBalancerRulesResponse (baseResponse):
    def __init__(self):
        """global load balancer rule ID"""
        self.id = None
        """the account of the load balancer rule"""
        self.account = None
        """the description of the global load balancer rule"""
        self.description = None
        """the domain of the load balancer rule"""
        self.domain = None
        """the domain ID of the load balancer rule"""
        self.domainid = None
        """DNS domain name given for the global load balancer"""
        self.gslbdomainname = None
        """Load balancing method used for the global load balancer"""
        self.gslblbmethod = None
        """GSLB service type"""
        self.gslbservicetype = None
        """session persistence method used for the global load balancer"""
        self.gslbstickysessionmethodname = None
        """name of the global load balancer rule"""
        self.name = None
        """the project name of the load balancer"""
        self.project = None
        """the project id of the load balancer"""
        self.projectid = None
        """Region Id in which global load balancer is created"""
        self.regionid = None
        """List of load balancer rules that are part of GSLB rule"""
        self.loadbalancerrule = []

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

class loadbalancerrule:
    def __init__(self):
        """"the load balancer rule ID"""
        self.id = None
        """"the account of the load balancer rule"""
        self.account = None
        """"the load balancer algorithm (source, roundrobin, leastconn)"""
        self.algorithm = None
        """"the cidr list to forward traffic from"""
        self.cidrlist = None
        """"the description of the load balancer"""
        self.description = None
        """"the domain of the load balancer rule"""
        self.domain = None
        """"the domain ID of the load balancer rule"""
        self.domainid = None
        """"the name of the load balancer"""
        self.name = None
        """"the id of the guest network the lb rule belongs to"""
        self.networkid = None
        """"the private port"""
        self.privateport = None
        """"the project name of the load balancer"""
        self.project = None
        """"the project id of the load balancer"""
        self.projectid = None
        """"the public ip address"""
        self.publicip = None
        """"the public ip address id"""
        self.publicipid = None
        """"the public port"""
        self.publicport = None
        """"the state of the rule"""
        self.state = None
        """"the id of the zone the rule belongs to"""
        self.zoneid = None
        """"the list of resource tags associated with load balancer"""
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

