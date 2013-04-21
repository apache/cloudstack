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
from marvin.cloudstackAPI import createTags
from marvin.cloudstackAPI import listTags
from marvin.cloudstackAPI import deleteTags

class Tags(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createTags.createTagsCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        tags = apiclient.createTags(cmd)
        return Tags(tags.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listTags.listTagsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        tags = apiclient.listTags(cmd)
        return map(lambda e: Tags(e.__dict__), tags)


    def delete(self, apiclient, resourcetype, resourceids, **kwargs):
        cmd = deleteTags.deleteTagsCmd()
        cmd.id = self.id
        cmd.resourceids = resourceids
        cmd.resourcetype = resourcetype
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        tags = apiclient.deleteTags(cmd)
        return tags
