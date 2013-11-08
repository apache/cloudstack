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


"""Assigns secondary IP to NIC"""
from baseCmd import *
from baseResponse import *
class addIpToNicCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the nic to which you want to assign private IP"""
        """Required"""
        self.nicid = None
        """Secondary IP Address"""
        self.ipaddress = None
        self.required = ["nicid",]

class addIpToNicResponse (baseResponse):
    def __init__(self):
        """the ID of the secondary private IP addr"""
        self.id = None
        """Secondary IP address"""
        self.ipaddress = None
        """the ID of the network"""
        self.networkid = None
        """the ID of the nic"""
        self.nicid = None
        """the ID of the vm"""
        self.virtualmachineid = None

