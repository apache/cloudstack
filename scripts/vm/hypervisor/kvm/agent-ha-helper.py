#!/usr/bin/env python3
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

import libvirt
import socket
import json
from http.server import BaseHTTPRequestHandler, HTTPServer

class Libvirt():
    def __init__(self):
        self.conn = libvirt.openReadOnly("qemu:///system")
        if not self.conn:
            raise Exception('Failed to open connection to libvirt')

    def running_vms(self):
        alldomains = [domain for domain in map(self.conn.lookupByID, self.conn.listDomainsID())]

        domains = []
        for domain in alldomains:
            if domain.info()[0] == libvirt.VIR_DOMAIN_RUNNING:
                domains.append(domain.name())
            elif domain.info()[0] == libvirt.VIR_DOMAIN_PAUSED:
                domains.append(domain.name())

        self.conn.close()

        return domains

class HTTPServerV6(HTTPServer):
    address_family = socket.AF_INET6

class CloudStackAgentHAHelper(BaseHTTPRequestHandler):
    def do_GET(self):

        if self.path != "/":
            self.send_response(404)
            self.end_headers()
            return

        libvirt = Libvirt()

        running_vms = libvirt.running_vms()

        output = {
            'count': len(running_vms),
            'virtualmachines': running_vms
        }

        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(output).encode())

def run(port=8080):
    server_address = ('', port)
    httpd = HTTPServerV6((server_address), CloudStackAgentHAHelper)
    httpd.serve_forever()

if __name__ == "__main__":
    from sys import argv

    try:
        if len(argv) == 2:
            run(port=int(argv[1]))
        else:
            run()
    except KeyboardInterrupt:
        pass
