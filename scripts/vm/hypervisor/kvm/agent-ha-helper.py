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
from base64 import b64encode

# Constants
APPLICATION_JSON = 'application/json'
BASIC_AUTH = "Basic "
CONTENT_TYPE = 'Content-type'
ROOT_PATH = '/'
CHECK_NEIGHBOUR_PATH = '/check-neighbour/'
CLOUD_KEY_PATH = '/etc/cloudstack/agent/cloud.key'
CLOUD_CERT_PATH = '/etc/cloudstack/agent/cloud.crt'
LOG_PATH = '/var/log/cloudstack/agent/agent-ha-helper.log'
HTTP_OK = 200
HTTP_MULTIPLE_CHOICES = 300
HTTP_UNAUTHORIZED = 401
HTTP_NOT_FOUND = 404
HTTP_PROTOCOL = 'http'
HTTPS_PROTOCOL = 'https'
QEMU_SYSTEM = 'qemu:///system'

# Variables
server_side = True
bind = ''
key = ''
insecure = False
port = 8443
http_protocol = HTTPS_PROTOCOL
username = 'kvmHaHelperDefaultUsername'
password = 'kvmhahelperDefaultPassword'

"""
    This web-server exposes a simple JSON API that returns a list of Virtual Machines running according to Libvirt.
    This helps on the CloudStack KVM HA as it van verify VMs status with HTTP-call to this simple webserver
        and determine if the host is actually down or if it is just the Java Agent which has crashed.
"""


class Libvirt():
    """
        Provides an interface to the libvirt, allowing to run commands in the libvirt.
        However, the scope of this webservice is restricted to LISTING VMs.
        Noneless, it would not just be out of scope from the KVM High Availability Client "needs" as well as it could
            expose security issues in case this webservice is accessed by malicious threats.
    """

    def __init__(self):
        self.conn = libvirt.openReadOnly(QEMU_SYSTEM)
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
    """
        Provides an HTTP client that can either be HTTP or HTTPS.
        Its execution allows either to list VMs on the current KVM node, or to call the same API from another HOST.
    """

    def do_GET(self):
        expected_header = BASIC_AUTH + key
        request_header = self.headers.get("Authorization")
        if request_header == expected_header:
            self.send_response(HTTP_OK)
            self.send_header(CONTENT_TYPE, APPLICATION_JSON)
            self.end_headers()
            self.process_get_request()
        else:
            logging.error('Failed to authenticate: wrong authentication method or credentials.')
            self.send_response(HTTP_UNAUTHORIZED)
            self.send_header(CONTENT_TYPE, APPLICATION_JSON)
            self.end_headers()

    def process_get_request(self):
        if self.path == ROOT_PATH:
            self.do_libvirt_vms_list()
        elif CHECK_NEIGHBOUR_PATH in self.path:
            self.do_libvirt_neighbour_check()
        else:
            self.send_response(HTTP_NOT_FOUND)
            self.end_headers()
            return

    def do_libvirt_vms_list(self):
        """ List Running VMs """
        libvirt = Libvirt()

        running_vms = libvirt.running_vms()

        output = {
            'count': len(running_vms),
            'virtualmachines': running_vms
        }

        self.send_response(HTTP_OK)
        self.send_header(CONTENT_TYPE, APPLICATION_JSON)
        self.end_headers()
        self.wfile.write(json.dumps(output).encode())

    def do_libvirt_neighbour_check(self):
        """
            Sends a request to the neighbour host, validating if it is healthy.
            Healthy Hosts respond by listing the expected number of VMs Running on the KVM.
            Unhealthy hosts fit in one of the following cases:
                (i) do not respond;
                (ii) respond with error code '-1';
                (iii) list '0' (zero) VMs when it was supposed to have multiple VMs running.
        """
        host_and_port = self.path.partition(CHECK_NEIGHBOUR_PATH)[2]
        request_url = '{}://{}/'.format(http_protocol, host_and_port)
        logging.debug('Check if Host {} is reachable via HTTPs GET request to agent-ha-helper.'.format(request_url))
        logging.debug('GET request: {}'.format(request_url))
        try:
            response = requests.get(url=request_url)
            if HTTP_OK <= response.status_code < HTTP_MULTIPLE_CHOICES:
                request_response = 'Up'
            else:
                request_response = 'Down'
        except:
            logging.error('GET Request {} failed.'.format(request_url))
            output = {
                'status': 'Down'
            }
            logging.debug('Neighbour host status:  {}'.format(output))
            self.send_response(HTTP_NOT_FOUND)
            self.send_header(CONTENT_TYPE, 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(output).encode())
            return

        logging.debug('Neighbour host status: {}'.format(request_response))
        output = {
            'status': request_response,
        }
        self.send_response(HTTP_OK)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(output).encode())

def run():
    """ Configure, create and serve the 'HTTP server' """
    server_address_and_port = (bind, port)
    httpd = HTTPServerV6((server_address_and_port), CloudStackAgentHAHelper)

    if insecure:
        logging.warning('Creating HTTP Server on insecure mode (HTTP, no SSL) exposed at port {}.'.format(port))
    elif not os.path.isfile(CLOUD_KEY_PATH) or not os.path.isfile(CLOUD_CERT_PATH):
        error_message = 'Failed to run HTTPS server, cannot find certificate or key files: "{}" or "{}".'.format(
            CLOUD_KEY_PATH, CLOUD_CERT_PATH)
        logging.error(error_message)
        raise Exception(error_message)
    else:
        logging.debug('Creating HTTPs Server at port {}.'.format(port))
        httpd.socket = ssl.wrap_socket(httpd.socket, keyfile=CLOUD_KEY_PATH, certfile=CLOUD_CERT_PATH)
    httpd.serve_forever()

def set_auth_key(username, password):
    """ Prepares Authentication key encoding the string '<username>:<password>' in base 64 """
    username_and_password = '{}:{}'.format(username, password)
    return b64encode(bytes(username_and_password, 'utf-8')).decode('utf-8')

if __name__ == "__main__":
    import argparse, sys

    logging.basicConfig(filename=LOG_PATH, format='%(asctime)s - %(message)s', level=logging.DEBUG)
    try:
        parser = argparse.ArgumentParser(prog='agent-ha-helper',
                                         usage='%(prog)s [-h] [-i] -p <port>',
                                         description='The agent-ha-helper.py provides a HTTP server '
                                                     'which handles API requests to identify '
                                                     'if the host (or a neighbour host) is healthy.')
        parser.add_argument('-i', '--insecure', help='Allows to run the HTTP server without SSL', action='store_true')
        parser.add_argument('-p', '--port', help='Port to be used by the agent-ha-helper server', type=int)
        parser.add_argument('-u', '--username', help='Sets the user for server authentication', type=str)
        parser.add_argument('-k', '--password', help='Keyword/password for server authentication', type=str)
        args = parser.parse_args()

        if not len(sys.argv) > 1:
            parser.print_help(sys.stderr)
            logging.warning(
                'Note: no arguments have been passed. Running with default configuration '
                '[bind:"::", port:{}, insecure:{}, username:{}, keyword:{}].'.format(port, insecure, username,
                                                                                     password))
        else:
            if args.insecure:
                insecure = True
                port = 8080
                http_protocol = HTTP_PROTOCOL
                logging.warning("WARNING: Insecure Mode turned ON!")

            if args.port is not None:
                port = args.port
            if args.username is not None:
                username = args.username
            if args.password is not None:
                password = args.password

        key = set_auth_key(username, password)
        run()

    except KeyboardInterrupt:
        pass
