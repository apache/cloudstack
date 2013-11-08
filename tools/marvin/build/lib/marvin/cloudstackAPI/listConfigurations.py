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


"""Lists all configurations."""
from baseCmd import *
from baseResponse import *
class listConfigurationsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the ID of the Account to update the parameter value for corresponding account"""
        self.accountid = None
        """lists configurations by category"""
        self.category = None
        """the ID of the Cluster to update the parameter value for corresponding cluster"""
        self.clusterid = None
        """List by keyword"""
        self.keyword = None
        """lists configuration by name"""
        self.name = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """the ID of the Storage pool to update the parameter value for corresponding storage pool"""
        self.storageid = None
        """the ID of the Zone to update the parameter value for corresponding zone"""
        self.zoneid = None
        self.required = []

class listConfigurationsResponse (baseResponse):
    def __init__(self):
        """the value of the configuration"""
        self.id = None
        """the category of the configuration"""
        self.category = None
        """the description of the configuration"""
        self.description = None
        """the name of the configuration"""
        self.name = None
        """scope(zone/cluster/pool/account) of the parameter that needs to be updated"""
        self.scope = None
        """the value of the configuration"""
        self.value = None

