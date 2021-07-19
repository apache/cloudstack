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

import logging
import libvirt
import socket
import json
import requests
from http.server import BaseHTTPRequestHandler, HTTPServer

log_folder = "/var/log/cloudstack/agent/"
log_path = "/var/log/cloudstack/agent/agent-ha-helper.log"
root_path = "/"
check_path = "/check-neighbour/"
http_ok = 200
http_multiple_choices = 300
http_not_found = 404

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
        if self.path == root_path:
            libvirt = Libvirt()

            running_vms = libvirt.running_vms()

            output = {
                'count': len(running_vms),
                'virtualmachines': running_vms
            }

            self.send_response(http_ok)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(output).encode())

        elif check_path in self.path:
            host_and_port = self.path.partition(check_path)[2]
            request_url = 'http://{}/'.format(host_and_port)
            logging.debug('Check if Host {} is reachable via HTTP GET request to agent-ha-helper.'.format(request_url))
            logging.debug('GET request: {}'.format(request_url))
            try:
                response = requests.get(url = request_url)
                if http_ok <= response.status_code < http_multiple_choices:
                    request_response = 'Up'
                else:
                    request_response = 'Down'
            except:
                logging.error('GET Request {} failed.'.format(request_url))
                output = {
                    'status': 'Down'
                }
                logging.debug('Neighbour host status:  {}'.format(output))
                self.send_response(http_not_found)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(output).encode())
                return

            logging.debug('Neighbour host status: {}'.format(request_response))
            output = {
                'status': request_response,
            }
            self.send_response(http_ok)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(output).encode())

        else:
            self.send_response(http_not_found)
            self.end_headers()
            return

def run(port=8080):
    server_address = ('', port)
    httpd = HTTPServerV6((server_address), CloudStackAgentHAHelper)
    httpd.serve_forever()


if __name__ == "__main__":
    from sys import argv
    logging.basicConfig(filename='/var/log/cloudstack/agent/agent-ha-helper.log', format='%(asctime)s - %(message)s', level=logging.DEBUG)
    try:
        if len(argv) == 2:
            run(port=int(argv[1]))
        else:
            run()
    except KeyboardInterrupt:
        pass
