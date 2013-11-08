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


"""Lists traffic types of a given physical network."""
from baseCmd import *
from baseResponse import *
class listTrafficTypesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the Physical Network ID"""
        """Required"""
        self.physicalnetworkid = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = ["physicalnetworkid",]

class listTrafficTypesResponse (baseResponse):
    def __init__(self):
        """uuid of the network provider"""
        self.id = None
        """true if individual services can be enabled/disabled"""
        self.canenableindividualservice = None
        """the destination physical network"""
        self.destinationphysicalnetworkid = None
        """the provider name"""
        self.name = None
        """the physical network this belongs to"""
        self.physicalnetworkid = None
        """services for this provider"""
        self.servicelist = None
        """state of the network provider"""
        self.state = None

