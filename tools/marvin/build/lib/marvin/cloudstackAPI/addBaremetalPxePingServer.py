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


"""add a baremetal ping pxe server"""
from baseCmd import *
from baseResponse import *
class addBaremetalPxePingServerCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """Credentials to reach external pxe device"""
        """Required"""
        self.password = None
        """the Physical Network ID"""
        """Required"""
        self.physicalnetworkid = None
        """Root directory on PING storage server"""
        """Required"""
        self.pingdir = None
        """PING storage server ip"""
        """Required"""
        self.pingstorageserverip = None
        """type of pxe device"""
        """Required"""
        self.pxeservertype = None
        """Tftp root directory of PXE server"""
        """Required"""
        self.tftpdir = None
        """URL of the external pxe device"""
        """Required"""
        self.url = None
        """Credentials to reach external pxe device"""
        """Required"""
        self.username = None
        """Password of PING storage server"""
        self.pingcifspassword = None
        """Username of PING storage server"""
        self.pingcifsusername = None
        """Pod Id"""
        self.podid = None
        self.required = ["password","physicalnetworkid","pingdir","pingstorageserverip","pxeservertype","tftpdir","url","username",]

class addBaremetalPxePingServerResponse (baseResponse):
    def __init__(self):
        """Root directory on PING storage server"""
        self.pingdir = None
        """PING storage server ip"""
        self.pingstorageserverip = None
        """Tftp root directory of PXE server"""
        self.tftpdir = None

