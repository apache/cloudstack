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


"""Recalculate and update resource count for an account or domain."""
from baseCmd import *
from baseResponse import *
class updateResourceCountCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """If account parameter specified then updates resource counts for a specified account in this domain else update resource counts for all accounts & child domains in specified domain."""
        """Required"""
        self.domainid = None
        """Update resource count for a specified account. Must be used with the domainId parameter."""
        self.account = None
        """Update resource limits for project"""
        self.projectid = None
        """Type of resource to update. If specifies valid values are 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 and 11. If not specified will update all resource counts0 - Instance. Number of instances a user can create. 1 - IP. Number of public IP addresses a user can own. 2 - Volume. Number of disk volumes a user can create.3 - Snapshot. Number of snapshots a user can create.4 - Template. Number of templates that a user can register/create.5 - Project. Number of projects that a user can create.6 - Network. Number of guest network a user can create.7 - VPC. Number of VPC a user can create.8 - CPU. Total number of CPU cores a user can use.9 - Memory. Total Memory (in MB) a user can use.10 - PrimaryStorage. Total primary storage space (in GiB) a user can use.11 - SecondaryStorage. Total secondary storage space (in GiB) a user can use."""
        self.resourcetype = None
        self.required = ["domainid",]

class updateResourceCountResponse (baseResponse):
    def __init__(self):
        """the account for which resource count's are updated"""
        self.account = None
        """the domain name for which resource count's are updated"""
        self.domain = None
        """the domain ID for which resource count's are updated"""
        self.domainid = None
        """the project name for which resource count's are updated"""
        self.project = None
        """the project id for which resource count's are updated"""
        self.projectid = None
        """resource count"""
        self.resourcecount = None
        """resource type. Values include 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11. See the resourceType parameter for more information on these values."""
        self.resourcetype = None

