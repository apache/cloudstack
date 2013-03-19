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
from marvin.cloudstackAPI import enableAccount
from marvin.cloudstackAPI import lockAccount
from marvin.cloudstackAPI import createAccount
from marvin.cloudstackAPI import listAccounts
from marvin.cloudstackAPI import updateAccount
from marvin.cloudstackAPI import disableAccount
from marvin.cloudstackAPI import deleteAccount

class Account(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def enable(self, apiclient, **kwargs):
        cmd = enableAccount.enableAccountCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        account = apiclient.enableAccount(cmd)


    def lock(self, apiclient, account, domainid, **kwargs):
        cmd = lockAccount.lockAccountCmd()
        cmd.account = account
        cmd.domainid = domainid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        account = apiclient.lockAccount(cmd)


    @classmethod
    def create(cls, apiclient, AccountFactory, **kwargs):
        cmd = createAccount.createAccountCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in AccountFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.createAccount(cmd)
        return Account(account.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listAccounts.listAccountsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        account = apiclient.listAccounts(cmd)
        return map(lambda e: Account(e.__dict__), account)


    def update(self, apiclient, newname, **kwargs):
        cmd = updateAccount.updateAccountCmd()
        cmd.newname = newname
        [setattr(cmd, key, value) for key,value in kwargs.items]
        account = apiclient.updateAccount(cmd)


    def disable(self, apiclient, lock, **kwargs):
        cmd = disableAccount.disableAccountCmd()
        cmd.lock = lock
        [setattr(cmd, key, value) for key,value in kwargs.items]
        account = apiclient.disableAccount(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteAccount.deleteAccountCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        account = apiclient.deleteAccount(cmd)
