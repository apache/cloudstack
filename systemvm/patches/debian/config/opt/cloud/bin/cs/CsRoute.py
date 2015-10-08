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

    def __init__(self):
        self.table_prefix = "Table_"

    def get_tablename(self, name):
        return self.table_prefix + name

    def add_table(self, devicename):
        tablenumber = devicename[3:]
        tablename = self.get_tablename(devicename)
        str = "%s %s" % (tablenumber, tablename)
        filename = "/etc/iproute2/rt_tables"
        logging.info(
            "Adding route table: " + str + " to " + filename + " if not present ")
        CsHelper.addifmissing(filename, str)

    def flush_table(self, tablename):
        CsHelper.execute("ip route flush table %s" % (tablename))
        CsHelper.execute("ip route flush cache")

    def add_route(self, dev, address):
        """ Wrapper method that adds table name and device to route statement """
        # ip route add dev eth1 table Table_eth1 10.0.2.0/24
        table = self.get_tablename(dev)
        logging.info("Adding route: dev " + dev + " table: " +
                     table + " network: " + address + " if not present")
        cmd = "dev %s table %s %s" % (dev, table, address)
        self.set_route(cmd)

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

    def add_defaultroute(self, gateway):
        """  Add a default route
        :param str gateway
        :return: bool
        """
        if not gateway:
            raise Exception("Gateway cannot be None.")
        
        if self.defaultroute_exists():
            return False
        else:
            cmd = "default via " + gateway
            logging.info("Adding default route")
            self.set_route(cmd)
            return True

    def defaultroute_exists(self):
        """ Return True if a default route is present
        :return: bool
        """
        logging.info("Checking if default ipv4 route is present")
        route_found = CsHelper.execute("ip -4 route list 0/0")

        if len(route_found) > 0:
            logging.info("Default route found: " + route_found[0])
            return True
        else:
            logging.warn("No default route found!")
            return False
