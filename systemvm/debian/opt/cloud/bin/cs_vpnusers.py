# -- coding: utf-8 --
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import copy


def merge(dbag, data):
    dbagc = copy.deepcopy(dbag)

    print(dbag)
    print(data)
    if "vpn_users" not in data:
        return dbagc

    # remove previously deleted user from the dict
    for user in list(dbagc.keys()):
        if user == 'id':
            continue
        userrec = dbagc[user]
        add = userrec['add']
        if not add:
            del(dbagc[user])

    for user in data['vpn_users']:
        username = user['user']
        add = user['add']
        if username not in list(dbagc.keys()):
            dbagc[username] = user
        elif username in list(dbagc.keys()) and not add:
            dbagc[username] = user

    return dbagc
