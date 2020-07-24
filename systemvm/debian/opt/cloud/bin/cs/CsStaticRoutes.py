#!/usr/bin/python3
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


class CsStaticRoutes(CsDataBag):

    def process(self):
        logging.debug("Processing CsStaticRoutes file ==> %s" % self.dbag)
        for item in self.dbag:
            if item == "id":
                continue
            self.__update(self.dbag[item])

    def __update(self, route):
        if route['revoke']:
            command = "ip route del %s via %s" % (route['network'], route['gateway'])
            CsHelper.execute(command)
        else:
            command = "ip route show | grep %s | awk '{print $1, $3}'" % route['network']
            result = CsHelper.execute(command)
            if not result:
                route_command = "ip route add %s via %s" % (route['network'], route['gateway'])
                CsHelper.execute(route_command)
