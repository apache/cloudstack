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


"""Lists dedicated pods."""
from baseCmd import *
from baseResponse import *
class listDedicatedPodsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the name of the account associated with the pod. Must be used with domainId."""
        self.account = None
        """list dedicated pods by affinity group"""
        self.affinitygroupid = None
        """the ID of the domain associated with the pod"""
        self.domainid = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """the ID of the pod"""
        self.podid = None
        self.required = []

class listDedicatedPodsResponse (baseResponse):
    def __init__(self):
        """the ID of the dedicated resource"""
        self.id = None
        """the Account Id to which the Pod is dedicated"""
        self.accountid = None
        """the Dedication Affinity Group ID of the pod"""
        self.affinitygroupid = None
        """the domain ID to which the Pod is dedicated"""
        self.domainid = None
        """the ID of the Pod"""
        self.podid = None
        """the Name of the Pod"""
        self.podname = None

