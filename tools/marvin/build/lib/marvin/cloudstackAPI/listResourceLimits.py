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


"""Lists resource limits."""
from baseCmd import *
from baseResponse import *
class listResourceLimitsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """Lists resource limits by ID."""
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
        """Type of resource to update. Values are 0, 1, 2, 3, and 4.0 - Instance. Number of instances a user can create. 1 - IP. Number of public IP addresses an account can own. 2 - Volume. Number of disk volumes an account can own.3 - Snapshot. Number of snapshots an account can own.4 - Template. Number of templates an account can register/create.5 - Project. Number of projects an account can own.6 - Network. Number of networks an account can own.7 - VPC. Number of VPC an account can own.8 - CPU. Number of CPU an account can allocate for his resources.9 - Memory. Amount of RAM an account can allocate for his resources.10 - Primary Storage. Amount of Primary storage an account can allocate for his resoruces.11 - Secondary Storage. Amount of Secondary storage an account can allocate for his resources."""
        self.resourcetype = None
        self.required = []

class listResourceLimitsResponse (baseResponse):
    def __init__(self):
        """the account of the resource limit"""
        self.account = None
        """the domain name of the resource limit"""
        self.domain = None
        """the domain ID of the resource limit"""
        self.domainid = None
        """the maximum number of the resource. A -1 means the resource currently has no limit."""
        self.max = None
        """the project name of the resource limit"""
        self.project = None
        """the project id of the resource limit"""
        self.projectid = None
        """resource type. Values include 0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11. See the resourceType parameter for more information on these values."""
        self.resourcetype = None

