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
from marvin.cloudstackAPI import enableAccount
from marvin.cloudstackAPI import lockAccount
from marvin.cloudstackAPI import createAccount
from marvin.cloudstackAPI import listAccounts
from marvin.cloudstackAPI import updateAccount
from marvin.cloudstackAPI import disableAccount
from marvin.cloudstackAPI import deleteAccount
from marvin.cloudstackAPI import markDefaultZoneForAccount


class Account(CloudStackEntity.CloudStackEntity):

    def __init__(self, items):
        self.__dict__.update(items)


    def enable(self, apiclient, **kwargs):
        cmd = enableAccount.enableAccountCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.enableAccount(cmd)
        return account


    def lock(self, apiclient, account, domainid, **kwargs):
        cmd = lockAccount.lockAccountCmd()
        cmd.id = self.id
        cmd.account = account
        cmd.domainid = domainid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.lockAccount(cmd)
        return account


    @classmethod
    def create(cls, apiclient, factory, **kwargs):
        cmd = createAccount.createAccountCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.createAccount(cmd)
        return Account(account.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listAccounts.listAccountsCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.listAccounts(cmd)
        return map(lambda e: Account(e.__dict__), account)


    def update(self, apiclient, newname, **kwargs):
        cmd = updateAccount.updateAccountCmd()
        cmd.id = self.id
        cmd.newname = newname
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.updateAccount(cmd)
        return account


    def disable(self, apiclient, lock, **kwargs):
        cmd = disableAccount.disableAccountCmd()
        cmd.id = self.id
        cmd.lock = lock
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.disableAccount(cmd)
        return account


    def delete(self, apiclient, **kwargs):
        cmd = deleteAccount.deleteAccountCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        account = apiclient.deleteAccount(cmd)
        return account


    def mark(self, apiclient, zoneid, **kwargs):
        cmd = markDefaultZoneForAccount.markDefaultZoneForAccountCmd()
        cmd.id = self.id
        cmd.account = self.account
        cmd.domainid = self.domainid
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        defaultzoneforaccount = apiclient.markDefaultZoneForAccount(cmd)
        return defaultzoneforaccount
