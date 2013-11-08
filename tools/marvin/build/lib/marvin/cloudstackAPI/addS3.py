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


"""Adds S3"""
from baseCmd import *
from baseResponse import *
class addS3Cmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """S3 access key"""
        """Required"""
        self.accesskey = None
        """name of the template storage bucket"""
        """Required"""
        self.bucket = None
        """S3 secret key"""
        """Required"""
        self.secretkey = None
        """connection timeout (milliseconds)"""
        self.connectiontimeout = None
        """S3 host name"""
        self.endpoint = None
        """maximum number of times to retry on error"""
        self.maxerrorretry = None
        """socket timeout (milliseconds)"""
        self.sockettimeout = None
        """connect to the S3 endpoint via HTTPS?"""
        self.usehttps = None
        self.required = ["accesskey","bucket","secretkey",]

class addS3Response (baseResponse):
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

