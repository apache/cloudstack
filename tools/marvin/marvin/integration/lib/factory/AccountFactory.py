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

from marvin.integration.lib.base import Account
import hashlib
from marvin.integration.lib.factory.CloudStackBaseFactory import *
from marvin.integration.lib.utils import random_gen

class AccountFactory(CloudStackBaseFactory):

    FACTORY_FOR = Account.Account

    accounttype = 0
    firstname = factory.Sequence(lambda n: random_gen())
    lastname = factory.Sequence(lambda n: random_gen())
    email = factory.LazyAttribute(lambda e: '{0}.{1}@cloudstack.org'.format(e.firstname, e.lastname).lower())
    username = factory.Sequence(lambda n: random_gen())

    # Password Encoding
    mdf = hashlib.md5()
    mdf.update('password')
    password = mdf.hexdigest()


class AdminAccountFactory(AccountFactory):
    accounttype = 1


class DomainAdminFactory(AccountFactory):
    accounttype = 2
    domainid = None
