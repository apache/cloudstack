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


"""Create a new keypair and returns the private key"""
from baseCmd import *
from baseResponse import *
class createSSHKeyPairCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """Name of the keypair"""
        """Required"""
        self.name = None
        """an optional account for the ssh key. Must be used with domainId."""
        self.account = None
        """an optional domainId for the ssh key. If the account parameter is used, domainId must also be used."""
        self.domainid = None
        """an optional project for the ssh key"""
        self.projectid = None
        self.required = ["name",]

class createSSHKeyPairResponse (baseResponse):
    def __init__(self):
        """Fingerprint of the public key"""
        self.fingerprint = None
        """Name of the keypair"""
        self.name = None
        """Private key"""
        self.privatekey = None

