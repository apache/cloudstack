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


"""Extracts a template"""
from baseCmd import *
from baseResponse import *
class extractTemplateCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the template"""
        """Required"""
        self.id = None
        """the mode of extraction - HTTP_DOWNLOAD or FTP_UPLOAD"""
        """Required"""
        self.mode = None
        """the url to which the ISO would be extracted"""
        self.url = None
        """the ID of the zone where the ISO is originally located"""
        self.zoneid = None
        self.required = ["id","mode",]

class extractTemplateResponse (baseResponse):
    def __init__(self):
        """the id of extracted object"""
        self.id = None
        """the account id to which the extracted object belongs"""
        self.accountid = None
        """the time and date the object was created"""
        self.created = None
        """the upload id of extracted object"""
        self.extractId = None
        """the mode of extraction - upload or download"""
        self.extractMode = None
        """the name of the extracted object"""
        self.name = None
        """the state of the extracted object"""
        self.state = None
        """the status of the extraction"""
        self.status = None
        """type of the storage"""
        self.storagetype = None
        """the percentage of the entity uploaded to the specified location"""
        self.uploadpercentage = None
        """if mode = upload then url of the uploaded entity. if mode = download the url from which the entity can be downloaded"""
        self.url = None
        """zone ID the object was extracted from"""
        self.zoneid = None
        """zone name the object was extracted from"""
        self.zonename = None

