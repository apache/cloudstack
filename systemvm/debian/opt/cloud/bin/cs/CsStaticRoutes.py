#!/usr/bin/python
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

import logging
from . import CsHelper
from .CsDatabag import CsDataBag
from .CsRoute import CsRoute


class CsStaticRoutes(CsDataBag):

    def process(self):
        logging.debug("Processing CsStaticRoutes file ==> %s" % self.dbag)
        for item in self.dbag:
            if item == "id":
                continue
            self.__update(self.dbag[item])

    def __find_device_for_gateway(self, gateway_ip):
        """
        Find which ethernet device the gateway IP belongs to by checking
        if the gateway is in any of the configured interface subnets.
        Returns device name (e.g., 'eth2') or None if not found.
        """
        try:
            # Get all configured interfaces from the address databag
            interfaces = self.config.address().get_interfaces()

            for interface in interfaces:
                if not interface.is_added():
                    continue

                # Check if gateway IP is in this interface's subnet
                if interface.ip_in_subnet(gateway_ip):
                    return interface.get_device()

            logging.debug("No matching device found for gateway %s" % gateway_ip)
            return None
        except Exception as e:
            logging.error("Error finding device for gateway %s: %s" % (gateway_ip, e))
            return None

    def __update(self, route):
        network = route['network']
        gateway = route['gateway']

        if route['revoke']:
            # Delete from main table
            command = "ip route del %s via %s" % (network, gateway)
            CsHelper.execute(command)

            # Delete from PBR table if applicable
            device = self.__find_device_for_gateway(gateway)
            if device:
                cs_route = CsRoute()
                table_name = cs_route.get_tablename(device)
                command = "ip route del %s via %s table %s" % (network, gateway, table_name)
                CsHelper.execute(command)
                logging.info("Deleted static route %s via %s from PBR table %s" % (network, gateway, table_name))
        else:
            # Add to main table (existing logic)
            command = "ip route show | grep %s | awk '{print $1, $3}'" % network
            result = CsHelper.execute(command)
            if not result:
                route_command = "ip route add %s via %s" % (network, gateway)
                CsHelper.execute(route_command)
                logging.info("Added static route %s via %s to main table" % (network, gateway))

            # Add to PBR table if applicable
            device = self.__find_device_for_gateway(gateway)
            if device:
                cs_route = CsRoute()
                table_name = cs_route.get_tablename(device)
                # Check if route already exists in the PBR table
                check_command = "ip route show table %s | grep %s | awk '{print $1, $3}'" % (table_name, network)
                result = CsHelper.execute(check_command)
                if not result:
                    # Add route to the interface-specific table
                    route_command = "ip route add %s via %s dev %s table %s" % (network, gateway, device, table_name)
                    CsHelper.execute(route_command)
                    logging.info("Added static route %s via %s to PBR table %s" % (network, gateway, table_name))
            else:
                logging.info("Static route %s via %s added to main table only (no matching interface found for PBR table)" % (network, gateway))
