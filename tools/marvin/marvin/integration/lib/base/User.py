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
from marvin.cloudstackAPI import enableUser
from marvin.cloudstackAPI import getUser
from marvin.cloudstackAPI import lockUser
from marvin.cloudstackAPI import createUser
from marvin.cloudstackAPI import listUsers
from marvin.cloudstackAPI import updateUser
from marvin.cloudstackAPI import disableUser
from marvin.cloudstackAPI import deleteUser
from marvin.cloudstackAPI import registerUserKeys

class User(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def enable(self, apiclient, id, **kwargs):
        cmd = enableUser.enableUserCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.enableUser(cmd)
        return user


    def get(self, apiclient, userapikey, **kwargs):
        cmd = getUser.getUserCmd()
        cmd.id = self.id
        cmd.userapikey = userapikey
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.getUser(cmd)
        return user


    def lock(self, apiclient, id, **kwargs):
        cmd = lockUser.lockUserCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.lockUser(cmd)
        return user


    @classmethod
    def create(cls, apiclient, UserFactory, **kwargs):
        cmd = createUser.createUserCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in UserFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.createUser(cmd)
        return User(user.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listUsers.listUsersCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.listUsers(cmd)
        return map(lambda e: User(e.__dict__), user)


    def update(self, apiclient, id, **kwargs):
        cmd = updateUser.updateUserCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.updateUser(cmd)
        return user


    def disable(self, apiclient, id, **kwargs):
        cmd = disableUser.disableUserCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.disableUser(cmd)
        return user


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteUser.deleteUserCmd()
        cmd.id = self.id
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        user = apiclient.deleteUser(cmd)
        return user


    def register_userkeys(self, apiclient, **kwargs):
        cmd = registerUserKeys.registerUserKeysCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        userkeys = apiclient.registerUserKeys(cmd)
        return userkeys
