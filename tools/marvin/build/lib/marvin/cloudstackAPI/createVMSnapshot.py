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


"""Creates snapshot for a vm."""
from baseCmd import *
from baseResponse import *
class createVMSnapshotCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """The ID of the vm"""
        """Required"""
        self.virtualmachineid = None
        """The discription of the snapshot"""
        self.description = None
        """The display name of the snapshot"""
        self.name = None
        """snapshot memory if true"""
        self.snapshotmemory = None
        self.required = ["virtualmachineid",]

class createVMSnapshotResponse (baseResponse):
    def __init__(self):
        """the ID of the vm snapshot"""
        self.id = None
        """the account associated with the disk volume"""
        self.account = None
        """the create date of the vm snapshot"""
        self.created = None
        """indiates if this is current snapshot"""
        self.current = None
        """the description of the vm snapshot"""
        self.description = None
        """the display name of the vm snapshot"""
        self.displayname = None
        """the domain associated with the disk volume"""
        self.domain = None
        """the ID of the domain associated with the disk volume"""
        self.domainid = None
        """the name of the vm snapshot"""
        self.name = None
        """the parent ID of the vm snapshot"""
        self.parent = None
        """the parent displayName of the vm snapshot"""
        self.parentName = None
        """the project name of the vpn"""
        self.project = None
        """the project id of the vpn"""
        self.projectid = None
        """the state of the vm snapshot"""
        self.state = None
        """VM Snapshot type"""
        self.type = None
        """the vm ID of the vm snapshot"""
        self.virtualmachineid = None
        """the Zone ID of the vm snapshot"""
        self.zoneid = None

