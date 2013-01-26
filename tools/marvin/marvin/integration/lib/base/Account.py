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
from . import CloudStackEntity

class Account(CloudStackEntity):

    def __init__(self, items):
        self.__dict__.update(items)


    def enable(self, apiclient, **kwargs):
        pass

    def lock(self, apiclient, account, domainid, **kwargs):
        pass

    @classmethod
    def create(cls, apiclient, AccountFactory, **kwargs):
        cmd = createAccount.createAccountCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in AccountFactory.attributes()]
        [setattr(cmd, key, value) for key,value in kwargs.items]
        return Account(apiclient.createAccount(cmd).__dict__)

    @classmethod
    def list(cls, apiclient, **kwargs):
        pass

    def update(self, apiclient, newname, **kwargs):
        pass

    def disable(self, apiclient, lock, **kwargs):
        pass

    def delete(self, apiclient, id, **kwargs):
        pass
