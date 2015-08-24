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
import CsHelper
import logging


class CsRoute:

    """ Manage routes """

    def __init__(self, dev):
        self.dev = dev
        self.tableNo = dev[3:]
        self.table = "Table_%s" % (dev)

    def routeTable(self):
        str = "%s %s" % (self.tableNo, self.table)
        filename = "/etc/iproute2/rt_tables"
        logging.info(
            "Adding route table: " + str + " to " + filename + " if not present ")
        CsHelper.addifmissing(filename, str)

    def flush(self):
        CsHelper.execute("ip route flush table %s" % (self.table))
        CsHelper.execute("ip route flush cache")

    def add(self, address, method="add"):
        # ip route show dev eth1 table Table_eth1 10.0.2.0/24
        if(method == "add"):
            cmd = "dev %s table %s %s" % (self.dev, self.table, address['network'])
            self.set_route(cmd, method)

    def set_route(self, cmd, method="add"):
        """ Add a route if it is not already defined """
        found = False
        for i in CsHelper.execute("ip route show " + cmd):
            found = True
        if not found and method == "add":
            logging.info("Add " + cmd)
            cmd = "ip route add " + cmd
        elif found and method == "delete":
            logging.info("Delete " + cmd)
            cmd = "ip route delete " + cmd
        else:
            return
        CsHelper.execute(cmd)
