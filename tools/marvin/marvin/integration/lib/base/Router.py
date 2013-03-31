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
from marvin.cloudstackAPI import destroyRouter
from marvin.cloudstackAPI import listRouters
from marvin.cloudstackAPI import stopRouter
from marvin.cloudstackAPI import rebootRouter
from marvin.cloudstackAPI import startRouter

class Router(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def destroy(self, apiclient, id, **kwargs):
        cmd = destroyRouter.destroyRouterCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        router = apiclient.destroyRouter(cmd)
        return router


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listRouters.listRoutersCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        router = apiclient.listRouters(cmd)
        return map(lambda e: Router(e.__dict__), router)


    def stop(self, apiclient, id, **kwargs):
        cmd = stopRouter.stopRouterCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        router = apiclient.stopRouter(cmd)
        return router


    def reboot(self, apiclient, id, **kwargs):
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        router = apiclient.rebootRouter(cmd)
        return router


    def start(self, apiclient, id, **kwargs):
        cmd = startRouter.startRouterCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        router = apiclient.startRouter(cmd)
        return router
