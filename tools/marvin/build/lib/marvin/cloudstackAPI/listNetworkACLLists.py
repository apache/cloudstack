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


"""Lists all network ACLs"""
from baseCmd import *
from baseResponse import *
class listNetworkACLListsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """Lists network ACL with the specified ID."""
        self.id = None
        """List by keyword"""
        self.keyword = None
        """list network ACLs by specified name"""
        self.name = None
        """list network ACLs by network Id"""
        self.networkid = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list network ACLs by Vpc Id"""
        self.vpcid = None
        self.required = []

class listNetworkACLListsResponse (baseResponse):
    def __init__(self):
        """the ID of the ACL"""
        self.id = None
        """Description of the ACL"""
        self.description = None
        """the Name of the ACL"""
        self.name = None
        """Id of the VPC this ACL is associated with"""
        self.vpcid = None

