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


"""Lists all static routes"""
from baseCmd import *
from baseResponse import *
class listStaticRoutesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """list static routes by gateway id"""
        self.gatewayid = None
        """list static route by id"""
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
        """List resources by tags (key/value pairs)"""
        self.tags = []
        """list static routes by vpc id"""
        self.vpcid = None
        self.required = []

class listStaticRoutesResponse (baseResponse):
    def __init__(self):
        """the ID of static route"""
        self.id = None
        """the account associated with the static route"""
        self.account = None
        """static route CIDR"""
        self.cidr = None
        """the domain associated with the static route"""
        self.domain = None
        """the ID of the domain associated with the static route"""
        self.domainid = None
        """VPC gateway the route is created for"""
        self.gatewayid = None
        """the project name of the static route"""
        self.project = None
        """the project id of the static route"""
        self.projectid = None
        """the state of the static route"""
        self.state = None
        """VPC the static route belongs to"""
        self.vpcid = None
        """the list of resource tags associated with static route"""
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

