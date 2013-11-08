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


"""Adds backup image store."""
from baseCmd import *
from baseResponse import *
class addImageStoreCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the image store provider name"""
        """Required"""
        self.provider = None
        """the details for the image store. Example: details[0].key=accesskey&details[0].value=s389ddssaa&details[1].key=secretkey&details[1].value=8dshfsss"""
        self.details = []
        """the name for the image store"""
        self.name = None
        """the URL for the image store"""
        self.url = None
        """the Zone ID for the image store"""
        self.zoneid = None
        self.required = ["provider",]

class addImageStoreResponse (baseResponse):
    def __init__(self):
        """the ID of the image store"""
        self.id = None
        """the details of the image store"""
        self.details = None
        """the name of the image store"""
        self.name = None
        """the protocol of the image store"""
        self.protocol = None
        """the provider name of the image store"""
        self.providername = None
        """the scope of the image store"""
        self.scope = None
        """the url of the image store"""
        self.url = None
        """the Zone ID of the image store"""
        self.zoneid = None
        """the Zone name of the image store"""
        self.zonename = None

