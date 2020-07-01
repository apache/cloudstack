#!/usr/bin/python3
# -*- coding: utf-8 -*-
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

import os
import sys
import uuid

from contextlib import closing
from optparse import OptionParser

try:
    import mysql.connector
except ImportError:
    print("mysql.connector cannot be imported, please install mysql-connector-python")
    sys.exit(1)

dryrun = False


def runSql(conn, query):
    if dryrun:
        print("Running SQL query: " + query)
        return
    with closing(conn.cursor()) as cursor:
        cursor.execute(query)


def migrateApiRolePermissions(apis, conn):
    # All allow for root admin role Admin(id:1)
    runSql(conn, "INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 1, '*', 'ALLOW', 0);")
    # Migrate rules based on commands.properties rule for ResourceAdmin(id:2), DomainAdmin(id:3), User(id:4)
    octetKey = {2:2, 3:4, 4:8}
    for role in [2, 3, 4]:
        sortOrder = 0
        for api in sorted(apis.keys()):
            # Ignore auth commands
            if api in ['login', 'logout', 'samlSso', 'samlSlo', 'listIdps', 'listAndSwitchSamlAccount', 'getSPMetadata']:
                continue
            if (octetKey[role] & int(apis[api])) > 0:
                runSql(conn, "INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), %d, '%s', 'ALLOW', %d);" % (role, api, sortOrder))
                sortOrder += 1
    print("Static role permissions from commands.properties have been migrated into the db")


def enableDynamicApiChecker(conn):
    runSql(conn, "UPDATE `cloud`.`configuration` SET value='true' where name='dynamic.apichecker.enabled'")
    conn.commit()
    conn.close()
    print("Dynamic role based API checker has been enabled!")


def main():
    parser = OptionParser()
    parser.add_option("-b", "--db", action="store", type="string", dest="db", default="cloud",
                        help="The name of the database, default: cloud")
    parser.add_option("-u", "--user", action="store", type="string", dest="user", default="cloud",
                        help="User name a MySQL user with privileges on cloud database")
    parser.add_option("-p", "--password", action="store", type="string", dest="password", default="cloud",
                        help="Password of a MySQL user with privileges on cloud database")
    parser.add_option("-H", "--host", action="store", type="string", dest="host", default="127.0.0.1",
                        help="Host or IP of the MySQL server")
    parser.add_option("-P", "--port", action="store", type="int", dest="port", default=3306,
                        help="Host or IP of the MySQL server")
    parser.add_option("-f", "--properties-file", action="store", type="string", dest="commandsfile", default="/etc/cloudstack/management/commands.properties",
                        help="The commands.properties file")
    parser.add_option("-D", "--default", action="store_true", dest="defaultRules", default=False,
                        help="")
    parser.add_option("-d", "--dryrun", action="store_true", dest="dryrun", default=False,
                        help="Dry run and debug operations this tool will perform")
    (options, args) = parser.parse_args()

    print("Apache CloudStack Role Permission Migration Tool")
    print("(c) Apache CloudStack Authors and the ASF, under the Apache License, Version 2.0\n")

    global dryrun
    if options.dryrun:
        dryrun = True

    conn = mysql.connector.connect(
            host=options.host,
            user=options.user,
            passwd=options.password,
            port=int(options.port),
            db=options.db)

    if options.defaultRules:
        print("Applying the default role permissions, ignoring any provided properties files(s).")
        enableDynamicApiChecker(conn)
        sys.exit(0)

    if not os.path.isfile(options.commandsfile):
        print("Provided commands.properties cannot be accessed or does not exist.")
        print("Please check passed options, or run only with --default option to use the default role permissions.")
        sys.exit(1)

    while True:
        choice = input("Running this migration tool will remove any " +
                           "default-role permissions from cloud.role_permissions. " +
                           "Do you want to continue? [y/N]").lower()
        if choice == 'y':
            break
        else:
            print("Aborting!")
            sys.exit(1)

    # Generate API to permission octet map
    apiMap = {}
    with open(options.commandsfile) as f:
        for line in f.readlines():
            if not line or line == '' or line == '\n' or line == '\r\n' or line.startswith('#'):
                continue
            name, value = line.split('=')
            apiMap[name.strip()] = value.strip().split(';')[-1]

    # Rename and deprecate old commands.properties file
    if not dryrun:
        os.rename(options.commandsfile, options.commandsfile + '.deprecated')
    print("The commands.properties file has been deprecated and moved at: " + options.commandsfile + '.deprecated')

    # Truncate any rules in cloud.role_permissions table
    runSql(conn, "DELETE FROM `cloud`.`role_permissions` WHERE `role_id` in (1,2,3,4);")

    # Migrate rules from commands.properties to cloud.role_permissions
    migrateApiRolePermissions(apiMap, conn)

    enableDynamicApiChecker(conn)

if __name__ == '__main__':
    main()
