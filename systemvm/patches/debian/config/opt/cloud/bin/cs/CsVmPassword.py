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

import CsHelper
from CsProcess import CsProcess
from netaddr import IPNetwork, IPAddress
import logging


class CsPassword:

    TOKEN_FILE="/tmp/passwdsrvrtoken"

    def __init__(self, dbag):
        self.dbag = dbag
        self.process()

    def process(self):
        self.__update(self.dbag['ip_address'], self.dbag['password'])

    def __update(self, vm_ip, password):
        token = ""
        try:
            tokenFile = open(self.TOKEN_FILE)
            token = tokenFile.read()
        except IOError:
            logging.debug("File %s does not exist" % self.TOKEN_FILE)

        logging.debug("Got VM '%s' and password '%s'" % (vm_ip, password))
        get_cidrs_cmd = "ip addr show | grep inet | grep -v secondary | awk '{print $2}'"
        cidrs = CsHelper.execute(get_cidrs_cmd)
        logging.debug("Found these CIDRs: %s" % cidrs)
        for cidr in cidrs:
            logging.debug("Processing CIDR '%s'" % cidr)
            if IPAddress(vm_ip) in IPNetwork(cidr):
                ip = cidr.split('/')[0]
                logging.debug("Cidr %s matches vm ip address %s so adding passwd to passwd server at %s" % (cidr, vm_ip, ip))
                proc = CsProcess(['/opt/cloud/bin/passwd_server_ip.py', ip])
                if proc.find():
                    update_command = 'curl --header "DomU_Request: save_password" "http://{SERVER_IP}:8080/" -F "ip={VM_IP}" -F "password={PASSWORD}" ' \
                                     '-F "token={TOKEN}" --interface 127.0.0.1 >/dev/null 2>/dev/null &'.format(SERVER_IP=ip, VM_IP=vm_ip, PASSWORD=password, TOKEN=token)
                    result = CsHelper.execute(update_command)
                    logging.debug("Update password server result ==> %s" % result)
                else:
                    logging.debug("Update password server skipped because we didn't find a passwd server process for %s (makes sense on backup routers)" % ip)
