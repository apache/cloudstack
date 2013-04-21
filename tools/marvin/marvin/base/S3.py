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
from marvin.base import CloudStackEntity
from marvin.cloudstackAPI import addS3
from marvin.cloudstackAPI import listS3s

class S3(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, secretkey, accesskey, bucket, **kwargs):
        cmd = addS3.addS3Cmd()
        cmd.id = self.id
        cmd.accesskey = accesskey
        cmd.bucket = bucket
        cmd.secretkey = secretkey
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        s3 = apiclient.addS3(cmd)
        return s3


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listS3s.listS3sCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        s3 = apiclient.listS3s(cmd)
        return map(lambda e: S3(e.__dict__), s3)
