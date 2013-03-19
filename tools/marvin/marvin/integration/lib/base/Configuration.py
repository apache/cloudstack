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
from marvin.cloudstackAPI import listConfigurations
from marvin.cloudstackAPI import updateConfiguration

class Configuration(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listConfigurations.listConfigurationsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        configuration = apiclient.listConfigurations(cmd)
        return map(lambda e: Configuration(e.__dict__), configuration)


    def update(self, apiclient, name, **kwargs):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        [setattr(cmd, key, value) for key,value in kwargs.items]
        configuration = apiclient.updateConfiguration(cmd)
