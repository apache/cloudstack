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


"""Creates a vm group"""
from baseCmd import *
from baseResponse import *
class createInstanceGroupCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the name of the instance group"""
        """Required"""
        self.name = None
        """the account of the instance group. The account parameter must be used with the domainId parameter."""
        self.account = None
        """the domain ID of account owning the instance group"""
        self.domainid = None
        """The project of the instance group"""
        self.projectid = None
        self.required = ["name",]

class createInstanceGroupResponse (baseResponse):
    def __init__(self):
        """the id of the instance group"""
        self.id = None
        """the account owning the instance group"""
        self.account = None
        """time and date the instance group was created"""
        self.created = None
        """the domain name of the instance group"""
        self.domain = None
        """the domain ID of the instance group"""
        self.domainid = None
        """the name of the instance group"""
        self.name = None
        """the project name of the group"""
        self.project = None
        """the project id of the group"""
        self.projectid = None

