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
import factory
from marvin.integration.lib.base import User
from marvin.integration.lib.factory.CloudStackBaseFactory import CloudStackBaseFactory
from marvin.integration.lib.factory.AccountFactory import AccountFactory

class UserFactory(CloudStackBaseFactory):

    FACTORY_FOR = User.User

    account = factory.RelatedFactory(AccountFactory).factory()
    email = account.email
    firstname = account.firstname
    lastname = account.lastname
    password = account.password
    username = account.username

class AdminUserFactory(UserFactory):
    account = factory.RelatedFactory(AccountFactory, accounttype=1).factory()
