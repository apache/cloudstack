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


"""Lists snapshot policies."""
from baseCmd import *
from baseResponse import *
class listSnapshotPoliciesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the ID of the disk volume"""
        """Required"""
        self.volumeid = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = ["volumeid",]

class listSnapshotPoliciesResponse (baseResponse):
    def __init__(self):
        """the ID of the snapshot policy"""
        self.id = None
        """the interval type of the snapshot policy"""
        self.intervaltype = None
        """maximum number of snapshots retained"""
        self.maxsnaps = None
        """time the snapshot is scheduled to be taken."""
        self.schedule = None
        """the time zone of the snapshot policy"""
        self.timezone = None
        """the ID of the disk volume"""
        self.volumeid = None

