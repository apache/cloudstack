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

class LdapTestData:
    #constants
    configuration = "ldap_configuration"
    syncAccounts = "accountsToSync"
    parentDomain = "LDAP"
    manualDomain = "manual"
    importDomain = "import"
    syncDomain = "sync"
    name = "name"
    id = "id"
    notAvailable = "N/A"
    groups = "groups"
    group = "group"
    seniorAccount = "seniors"
    juniorAccount = "juniors"

    ldap_ip_address = "localhost"
    ldap_port = "389"
    hostname = "hostname"
    port = "port"
    dn = "dn"
    ou = "ou"
    cn = "cn"
    member = "uniqueMember"
    basedn = "basedn"
    basednConfig = "ldap.basedn"
    ldapPw = "ldapPassword"
    ldapPwConfig = "ldap.bind.password"
    principal = "ldapUsername"
    principalConfig = "ldap.bind.principal"
    users = "users"
    objectClass = "objectClass"
    sn = "sn"
    givenName = "givenName"
    uid = "uid"
    domains = "domains"
    type = "accounttype"

    admins = "ou=admins,ou=groups,dc=echt,dc=net"
    juniors = "ou=juniors,ou=groups,dc=echt,dc=net"
    seniors = "ou=seniors,ou=groups,dc=echt,dc=net"

    def __init__(self):
        self.testdata = {
            LdapTestData.configuration: {
                "emailAttribute": "mail",
                "userObject": "person",
                "usernameAttribute": LdapTestData.uid,
                # global values for use in all domains
                LdapTestData.hostname: LdapTestData.ldap_ip_address,
                LdapTestData.port: LdapTestData.ldap_port,
                LdapTestData.basedn: "dc=echt,dc=net",
                LdapTestData.ldapPw: "secret",
                LdapTestData.principal: "cn=willie,dc=echt,dc=net",
            },
            LdapTestData.groups: [
                {
                    LdapTestData.dn : "ou=people,dc=echt,dc=net",
                    LdapTestData.objectClass: ["organizationalUnit", "top"],
                    LdapTestData.ou : "People"
                },
                {
                    LdapTestData.dn : "ou=groups,dc=echt,dc=net",
                    LdapTestData.objectClass: ["organizationalUnit", "top"],
                    LdapTestData.ou : "Groups"
                },
                {
                    LdapTestData.dn : LdapTestData.seniors,
                    LdapTestData.objectClass: ["groupOfUniqueNames", "top"],
                    LdapTestData.ou : "seniors",
                    LdapTestData.cn : "seniors",
                    LdapTestData.member : ["uid=bobby,ou=people,dc=echt,dc=net", "uid=rohit,ou=people,dc=echt,dc=net"]
                },
                {
                    LdapTestData.dn : LdapTestData.juniors,
                    LdapTestData.objectClass : ["groupOfUniqueNames", "top"],
                    LdapTestData.ou : "juniors",
                    LdapTestData.cn : "juniors",
                    LdapTestData.member : ["uid=dahn,ou=people,dc=echt,dc=net", "uid=paul,ou=people,dc=echt,dc=net"]
                }
            ],
            LdapTestData.users: [
                {
                    LdapTestData.dn : "uid=bobby,ou=people,dc=echt,dc=net",
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "bobby",
                    LdapTestData.sn: "Stoyanov",
                    LdapTestData.givenName : "Boris",
                    LdapTestData.uid : "bobby",
                    "email": "bobby@echt.net"
                },
                {
                    LdapTestData.dn : "uid=dahn,ou=people,dc=echt,dc=net",
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "dahn",
                    LdapTestData.sn: "Hoogland",
                    LdapTestData.givenName : "Daan",
                    LdapTestData.uid : "dahn",
                    "email": "dahn@echt.net"
                },
                {
                    LdapTestData.dn : "uid=paul,ou=people,dc=echt,dc=net",
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "Paul",
                    LdapTestData.sn: "Angus",
                    LdapTestData.givenName : "Paul",
                    LdapTestData.uid : "paul",
                    "email": "paul@echt.net"
                },
                {
                    LdapTestData.dn : "uid=rohit,ou=people,dc=echt,dc=net",
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "rhtyd",
                    LdapTestData.sn: "Yadav",
                    LdapTestData.givenName : "Rohit",
                    LdapTestData.uid : "rohit",
                    "email": "rhtyd@echt.net"
                },
            ],
            LdapTestData.domains : [
                {
                    LdapTestData.name : LdapTestData.parentDomain,
                    LdapTestData.id : LdapTestData.notAvailable
                },
                {
                    LdapTestData.name : LdapTestData.manualDomain,
                    LdapTestData.id : LdapTestData.notAvailable
                },
                {
                    LdapTestData.name : LdapTestData.importDomain,
                    LdapTestData.id : LdapTestData.notAvailable
                },
                {
                    LdapTestData.name : LdapTestData.syncDomain,
                    LdapTestData.id : LdapTestData.notAvailable
                },
            ],
            LdapTestData.syncAccounts : [
                {
                    LdapTestData.name : LdapTestData.juniorAccount,
                    LdapTestData.type : 0,
                    LdapTestData.group : LdapTestData.juniors
                },
                {
                    LdapTestData.name : LdapTestData.seniorAccount,
                    LdapTestData.type : 2,
                    LdapTestData.group : LdapTestData.seniors
                }
            ],
        }