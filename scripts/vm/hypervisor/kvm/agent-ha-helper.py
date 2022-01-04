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
import json
import socket
import requests
import ssl
import os.path
from http.server import HTTPServer, SimpleHTTPRequestHandler

root_path = "/"
check_path = "/check-neighbour/"
cloud_key = '/etc/cloudstack/agent/cloud.key'
cloud_cert = '/etc/cloudstack/agent/cloud.crt'
log_path = '/var/log/cloudstack/agent/agent-ha-helper.log'
http_ok = 200
http_multiple_choices = 300
http_not_found = 404
server_side = True
bind = ''
insecure = False
port=8443

class Libvirt():
    def __init__(self):
        self.conn = libvirt.openReadOnly("qemu:///system")
        if not self.conn:
            libvirt_error = 'Failed to open connection to libvirt'
            logging.error(libvirt_error)
            raise Exception(libvirt_error)

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

class CloudStackAgentHAHelper(SimpleHTTPRequestHandler):
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
            request_url = 'https://{}/'.format(host_and_port)
            logging.debug('Check if Host {} is reachable via HTTPs GET request to agent-ha-helper.'.format(request_url))
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

def run():
    server_address_and_port = (bind, port)
    httpd = HTTPServerV6((server_address_and_port), CloudStackAgentHAHelper)

    if insecure:
        logging.warning('Creating HTTP Server on insecure mode (HTTP, no SSL) exposed at port {}.'.format(port))
    elif not os.path.isfile(cloud_key) or not os.path.isfile(cloud_cert):
        error_message = 'Failed to run HTTPS server, cannot find certificate or key files: "{}" or "{}".'.format(cloud_key, cloud_cert)
        logging.error(error_message)
        raise Exception(error_message)
    else:
        logging.debug('Creating HTTPs Server at port {}.'.format(port))
        httpd.socket = ssl.wrap_socket(httpd.socket, keyfile=cloud_key, certfile=cloud_cert)
    httpd.serve_forever()

if __name__ == "__main__":
    import argparse, sys
    logging.basicConfig(filename=log_path, format='%(asctime)s - %(message)s', level=logging.DEBUG)
    try:
        parser = argparse.ArgumentParser(prog='agent-ha-helper',
                                         usage='%(prog)s [-h] [-i] -p <port>',
                                         description='The agent-ha-helper.py provides a HTTP server '
                                                     'which handles API requests to identify '
                                                     'if the host (or a neighbour host) is healthy.')
        parser.add_argument('-i', '--insecure', help='Allows to run the HTTP server without SSL', action='store_true')
        parser.add_argument('-p', '--port', help='Port to be used by the agent-ha-helper server', type=int)
        args = parser.parse_args()

        if not len(sys.argv) > 1:
            parser.print_help(sys.stderr)
            logging.warning('Note: no arguments have been passed. Using default values [bind: "::", port: {}, insecure: {}].'.format(port, insecure))
        else:
            if args.insecure:
                insecure = True
                port = 8080
                print("insecure turned on")

            if args.port is not None:
                port = args.port
        run()
    except KeyboardInterrupt:
        pass
