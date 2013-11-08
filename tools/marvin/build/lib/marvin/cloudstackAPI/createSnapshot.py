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


"""Creates an instant snapshot of a volume."""
from baseCmd import *
from baseResponse import *
class createSnapshotCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """The ID of the disk volume"""
        """Required"""
        self.volumeid = None
        """The account of the snapshot. The account parameter must be used with the domainId parameter."""
        self.account = None
        """The domain ID of the snapshot. If used with the account parameter, specifies a domain for the account associated with the disk volume."""
        self.domainid = None
        """policy id of the snapshot, if this is null, then use MANUAL_POLICY."""
        self.policyid = None
        self.required = ["volumeid",]

class createSnapshotResponse (baseResponse):
    def __init__(self):
        """ID of the snapshot"""
        self.id = None
        """the account associated with the snapshot"""
        self.account = None
        """the date the snapshot was created"""
        self.created = None
        """the domain name of the snapshot's account"""
        self.domain = None
        """the domain ID of the snapshot's account"""
        self.domainid = None
        """valid types are hourly, daily, weekly, monthy, template, and none."""
        self.intervaltype = None
        """name of the snapshot"""
        self.name = None
        """the project name of the snapshot"""
        self.project = None
        """the project id of the snapshot"""
        self.projectid = None
        """the type of the snapshot"""
        self.snapshottype = None
        """the state of the snapshot. BackedUp means that snapshot is ready to be used; Creating - the snapshot is being allocated on the primary storage; BackingUp - the snapshot is being backed up on secondary storage"""
        self.state = None
        """ID of the disk volume"""
        self.volumeid = None
        """name of the disk volume"""
        self.volumename = None
        """type of the disk volume"""
        self.volumetype = None
        """id of the availability zone"""
        self.zoneid = None
        """the list of resource tags associated with snapshot"""
        self.tags = []
        """the ID of the latest async job acting on this object"""
        self.jobid = None
        """the current status of the latest async job acting on this object"""
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

