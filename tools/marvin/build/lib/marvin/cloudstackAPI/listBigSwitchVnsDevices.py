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


"""Lists BigSwitch Vns devices"""
from baseCmd import *
from baseResponse import *
class listBigSwitchVnsDevicesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """the Physical Network ID"""
        self.physicalnetworkid = None
        """bigswitch vns device ID"""
        self.vnsdeviceid = None
        self.required = []

class listBigSwitchVnsDevicesResponse (baseResponse):
    def __init__(self):
        """device name"""
        self.bigswitchdevicename = None
        """the controller Ip address"""
        self.hostname = None
        """the physical network to which this BigSwitch Vns belongs to"""
        self.physicalnetworkid = None
        """name of the provider"""
        self.provider = None
        """device id of the BigSwitch Vns"""
        self.vnsdeviceid = None

