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


"""Lists all available disk offerings."""
from baseCmd import *
from baseResponse import *
class listDiskOfferingsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the ID of the domain of the disk offering."""
        self.domainid = None
        """ID of the disk offering"""
        self.id = None
        """List by keyword"""
        self.keyword = None
        """name of the disk offering"""
        self.name = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = []

class listDiskOfferingsResponse (baseResponse):
    def __init__(self):
        """unique ID of the disk offering"""
        self.id = None
        """the date this disk offering was created"""
        self.created = None
        """bytes read rate of the disk offering"""
        self.diskBytesReadRate = None
        """bytes write rate of the disk offering"""
        self.diskBytesWriteRate = None
        """io requests read rate of the disk offering"""
        self.diskIopsReadRate = None
        """io requests write rate of the disk offering"""
        self.diskIopsWriteRate = None
        """the size of the disk offering in GB"""
        self.disksize = None
        """whether to display the offering to the end user or not."""
        self.displayoffering = None
        """an alternate display text of the disk offering."""
        self.displaytext = None
        """the domain name this disk offering belongs to. Ignore this information as it is not currently applicable."""
        self.domain = None
        """the domain ID this disk offering belongs to. Ignore this information as it is not currently applicable."""
        self.domainid = None
        """true if disk offering uses custom size, false otherwise"""
        self.iscustomized = None
        """true if disk offering uses custom iops, false otherwise"""
        self.iscustomizediops = None
        """the max iops of the disk offering"""
        self.maxiops = None
        """the min iops of the disk offering"""
        self.miniops = None
        """the name of the disk offering"""
        self.name = None
        """the storage type for this disk offering"""
        self.storagetype = None
        """the tags for the disk offering"""
        self.tags = None

