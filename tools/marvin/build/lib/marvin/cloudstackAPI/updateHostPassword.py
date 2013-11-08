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


"""Update password of a host/pool on management server."""
from baseCmd import *
from baseResponse import *
class updateHostPasswordCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the new password for the host/cluster"""
        """Required"""
        self.password = None
        """the username for the host/cluster"""
        """Required"""
        self.username = None
        """the cluster ID"""
        self.clusterid = None
        """the host ID"""
        self.hostid = None
        self.required = ["password","username",]

class updateHostPasswordResponse (baseResponse):
    def __init__(self):
        """any text associated with the success or failure"""
        self.displaytext = None
        """true if operation is executed successfully"""
        self.success = None

