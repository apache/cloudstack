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
from marvin.cloudstackAPI import listTemplatePermissions
from marvin.cloudstackAPI import updateTemplatePermissions

class TemplatePermissions(CloudStackEntity.CloudStackEntity):


    def __init__(self, **kwargs):
        self.__dict__.update(**kwargs)


    @classmethod
    def list(self, apiclient, id, **kwargs):
        cmd = listTemplatePermissions.listTemplatePermissionsCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        templatepermissions = apiclient.listTemplatePermissions(cmd)
        return map(lambda e: TemplatePermissions(e.__dict__), templatepermissions)


    def update(self, apiclient, id, **kwargs):
        cmd = updateTemplatePermissions.updateTemplatePermissionsCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        templatepermissions = apiclient.updateTemplatePermissions(cmd)
