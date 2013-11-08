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


"""Deletes security group"""
from baseCmd import *
from baseResponse import *
class deleteSecurityGroupCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the account of the security group. Must be specified with domain ID"""
        self.account = None
        """the domain ID of account owning the security group"""
        self.domainid = None
        """The ID of the security group. Mutually exclusive with name parameter"""
        self.id = None
        """The ID of the security group. Mutually exclusive with id parameter"""
        self.name = None
        """the project of the security group"""
        self.projectid = None
        self.required = []

class deleteSecurityGroupResponse (baseResponse):
    def __init__(self):
        """any text associated with the success or failure"""
        self.displaytext = None
        """true if operation is executed successfully"""
        self.success = None

