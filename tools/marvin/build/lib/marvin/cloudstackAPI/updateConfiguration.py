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


"""Updates a configuration."""
from baseCmd import *
from baseResponse import *
class updateConfigurationCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the name of the configuration"""
        """Required"""
        self.name = None
        """the ID of the Account to update the parameter value for corresponding account"""
        self.accountid = None
        """the ID of the Cluster to update the parameter value for corresponding cluster"""
        self.clusterid = None
        """the ID of the Storage pool to update the parameter value for corresponding storage pool"""
        self.storageid = None
        """the value of the configuration"""
        self.value = None
        """the ID of the Zone to update the parameter value for corresponding zone"""
        self.zoneid = None
        self.required = ["name",]

class updateConfigurationResponse (baseResponse):
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

