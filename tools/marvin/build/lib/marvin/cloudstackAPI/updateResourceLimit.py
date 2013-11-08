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


"""Updates resource limits for an account or domain."""
from baseCmd import *
from baseResponse import *
class updateResourceLimitCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """Type of resource to update. Values are 0, 1, 2, 3, 4, 6, 7, 8, 9, 10 and 11. 0 - Instance. Number of instances a user can create. 1 - IP. Number of public IP addresses a user can own. 2 - Volume. Number of disk volumes a user can create.3 - Snapshot. Number of snapshots a user can create.4 - Template. Number of templates that a user can register/create.6 - Network. Number of guest network a user can create.7 - VPC. Number of VPC a user can create.8 - CPU. Total number of CPU cores a user can use.9 - Memory. Total Memory (in MB) a user can use.10 - PrimaryStorage. Total primary storage space (in GiB) a user can use.11 - SecondaryStorage. Total secondary storage space (in GiB) a user can use."""
        """Required"""
        self.resourcetype = None
        """Update resource for a specified account. Must be used with the domainId parameter."""
        self.account = None
        """Update resource limits for all accounts in specified domain. If used with the account parameter, updates resource limits for a specified account in specified domain."""
        self.domainid = None
        """Maximum resource limit."""
        self.max = None
        """Update resource limits for project"""
        self.projectid = None
        self.required = ["resourcetype",]

class updateResourceLimitResponse (baseResponse):
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

