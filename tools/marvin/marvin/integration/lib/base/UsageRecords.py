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
from marvin.integration.lib.base import CloudStackEntity
from marvin.cloudstackAPI import listUsageRecords
from marvin.cloudstackAPI import generateUsageRecords

class UsageRecords(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def list(self, apiclient, startdate, enddate, **kwargs):
        cmd = listUsageRecords.listUsageRecordsCmd()
        cmd.enddate = enddate
        cmd.startdate = startdate
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        usagerecords = apiclient.listUsageRecords(cmd)
        return map(lambda e: UsageRecords(e.__dict__), usagerecords)


    def generate(self, apiclient, startdate, enddate, **kwargs):
        cmd = generateUsageRecords.generateUsageRecordsCmd()
        cmd.id = self.id
        cmd.enddate = enddate
        cmd.startdate = startdate
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        usagerecords = apiclient.generateUsageRecords(cmd)
        return usagerecords
