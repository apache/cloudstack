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
    ldap_port = 389
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
    password = "userPassword"
    mail = "email"
    groupPrinciple = "ldap.search.group.principle"

    basednValue = "dc=echt,dc=net"
    people_dn = "ou=people,"+basednValue
    groups_dn = "ou=groups,"+basednValue
    admins = "ou=admins,"+groups_dn
    juniors = "ou=juniors,"+groups_dn
    seniors = "ou=seniors,"+groups_dn
    userObject = "userObject"
    usernameAttribute = "usernameAttribute"
    memberAttribute = "memberAttribute"
    mailAttribute = "emailAttribute"

    def __init__(self):
        self.testdata = {
            LdapTestData.configuration: {
                LdapTestData.mailAttribute: "mail",
                LdapTestData.userObject: "person",
                LdapTestData.usernameAttribute: LdapTestData.uid,
                LdapTestData.memberAttribute: LdapTestData.member,
                # global values for use in all domains
                LdapTestData.hostname: LdapTestData.ldap_ip_address,
                LdapTestData.port: LdapTestData.ldap_port,
                LdapTestData.basedn: LdapTestData.basednValue,
                LdapTestData.ldapPw: "secret",
                LdapTestData.principal: "cn=willie,"+LdapTestData.basednValue,
            },
            LdapTestData.groups: [
                {
                    LdapTestData.dn : LdapTestData.people_dn,
                    LdapTestData.objectClass: ["organizationalUnit", "top"],
                    LdapTestData.ou : "People"
                },
                {
                    LdapTestData.dn : LdapTestData.groups_dn,
                    LdapTestData.objectClass: ["organizationalUnit", "top"],
                    LdapTestData.ou : "Groups"
                },
                {
                    LdapTestData.dn : LdapTestData.seniors,
                    LdapTestData.objectClass: ["groupOfUniqueNames", "top"],
                    LdapTestData.ou : "seniors",
                    LdapTestData.cn : "seniors",
                    LdapTestData.member : ["uid=bobby,ou=people,"+LdapTestData.basednValue, "uid=rohit,ou=people,"+LdapTestData.basednValue]
                },
                {
                    LdapTestData.dn : LdapTestData.juniors,
                    LdapTestData.objectClass : ["groupOfUniqueNames", "top"],
                    LdapTestData.ou : "juniors",
                    LdapTestData.cn : "juniors",
                    LdapTestData.member : ["uid=dahn,ou=people,"+LdapTestData.basednValue, "uid=paul,ou=people,"+LdapTestData.basednValue]
                }
            ],
            LdapTestData.users: [
                {
                    LdapTestData.dn : "uid=bobby,ou=people,"+LdapTestData.basednValue,
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "bobby",
                    LdapTestData.sn: "Stoyanov",
                    LdapTestData.givenName : "Boris",
                    LdapTestData.uid : "bobby",
                    LdapTestData.mail: "bobby@echt.net"
                },
                {
                    LdapTestData.dn : "uid=dahn,ou=people,"+LdapTestData.basednValue,
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "dahn",
                    LdapTestData.sn: "Hoogland",
                    LdapTestData.givenName : "Daan",
                    LdapTestData.uid : "dahn",
                    LdapTestData.mail: "dahn@echt.net"
                },
                {
                    LdapTestData.dn : "uid=paul,ou=people,"+LdapTestData.basednValue,
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "Paul",
                    LdapTestData.sn: "Angus",
                    LdapTestData.givenName : "Paul",
                    LdapTestData.uid : "paul",
                    LdapTestData.mail: "paul@echt.net"
                },
                {
                    LdapTestData.dn : "uid=rohit,ou=people,"+LdapTestData.basednValue,
                    LdapTestData.objectClass : ["inetOrgPerson", "top", "person"],
                    LdapTestData.cn : "rhtyd",
                    LdapTestData.sn: "Yadav",
                    LdapTestData.givenName : "Rohit",
                    LdapTestData.uid : "rohit",
                    LdapTestData.mail: "rhtyd@echt.net"
                },
                # extra test user (just in case)
                # {
                #     LdapTestData.dn : "uid=noone,ou=people,"+LdapTestData.basednValue,
                #     LdapTestData.objectClass : ["inetOrgPerson", "person"],
                #     LdapTestData.cn : "noone",
                #     LdapTestData.sn: "a User",
                #     LdapTestData.givenName : "Not",
                #     LdapTestData.uid : "noone",
                #     LdapTestData.mail: "noone@echt.net",
                #     LdapTestData.password: 'password'
                # },
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
