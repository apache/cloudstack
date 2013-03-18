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
from marvin.cloudstackAPI import resetApiLimit
from marvin.cloudstackAPI import getApiLimit

class ApiLimit(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    def reset(self, apiclient, **kwargs):
        cmd = resetApiLimit.resetApiLimitCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        apilimit = apiclient.resetApiLimit(cmd)


    def get(self, apiclient, **kwargs):
        cmd = getApiLimit.getApiLimitCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        apilimit = apiclient.getApiLimit(cmd)

