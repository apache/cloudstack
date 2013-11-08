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


"""Creates a load balancer rule"""
from baseCmd import *
from baseResponse import *
class createLoadBalancerRuleCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """load balancer algorithm (source, roundrobin, leastconn)"""
        """Required"""
        self.algorithm = None
        """name of the load balancer rule"""
        """Required"""
        self.name = None
        """the private port of the private ip address/virtual machine where the network traffic will be load balanced to"""
        """Required"""
        self.privateport = None
        """the public port from where the network traffic will be load balanced from"""
        """Required"""
        self.publicport = None
        """the account associated with the load balancer. Must be used with the domainId parameter."""
        self.account = None
        """the cidr list to forward traffic from"""
        self.cidrlist = []
        """the description of the load balancer rule"""
        self.description = None
        """the domain ID associated with the load balancer"""
        self.domainid = None
        """The guest network this rule will be created for. Required when public Ip address is not associated with any Guest network yet (VPC case)"""
        self.networkid = None
        """if true, firewall rule for source/end pubic port is automatically created; if false - firewall rule has to be created explicitely. If not specified 1) defaulted to false when LB rule is being created for VPC guest network 2) in all other cases defaulted to true"""
        self.openfirewall = None
        """public ip address id from where the network traffic will be load balanced from"""
        self.publicipid = None
        """zone where the load balancer is going to be created. This parameter is required when LB service provider is ElasticLoadBalancerVm"""
        self.zoneid = None
        self.required = ["algorithm","name","privateport","publicport",]

class createLoadBalancerRuleResponse (baseResponse):
    def __init__(self):
        """the load balancer rule ID"""
        self.id = None
        """the account of the load balancer rule"""
        self.account = None
        """the load balancer algorithm (source, roundrobin, leastconn)"""
        self.algorithm = None
        """the cidr list to forward traffic from"""
        self.cidrlist = None
        """the description of the load balancer"""
        self.description = None
        """the domain of the load balancer rule"""
        self.domain = None
        """the domain ID of the load balancer rule"""
        self.domainid = None
        """the name of the load balancer"""
        self.name = None
        """the id of the guest network the lb rule belongs to"""
        self.networkid = None
        """the private port"""
        self.privateport = None
        """the project name of the load balancer"""
        self.project = None
        """the project id of the load balancer"""
        self.projectid = None
        """the public ip address"""
        self.publicip = None
        """the public ip address id"""
        self.publicipid = None
        """the public port"""
        self.publicport = None
        """the state of the rule"""
        self.state = None
        """the id of the zone the rule belongs to"""
        self.zoneid = None
        """the list of resource tags associated with load balancer"""
        self.tags = []

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

