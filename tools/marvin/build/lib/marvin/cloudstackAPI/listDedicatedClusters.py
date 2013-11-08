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


"""Lists dedicated clusters."""
from baseCmd import *
from baseResponse import *
class listDedicatedClustersCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the name of the account associated with the cluster. Must be used with domainId."""
        self.account = None
        """list dedicated clusters by affinity group"""
        self.affinitygroupid = None
        """the ID of the cluster"""
        self.clusterid = None
        """the ID of the domain associated with the cluster"""
        self.domainid = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = []

class listDedicatedClustersResponse (baseResponse):
    def __init__(self):
        """the ID of the dedicated resource"""
        self.id = None
        """the Account ID of the cluster"""
        self.accountid = None
        """the Dedication Affinity Group ID of the cluster"""
        self.affinitygroupid = None
        """the ID of the cluster"""
        self.clusterid = None
        """the name of the cluster"""
        self.clustername = None
        """the domain ID of the cluster"""
        self.domainid = None

