#Licensed to the Apache Software Foundation (ASF) under one
#or more contributor license agreements.  See the NOTICE file
#distributed with this work for additional information
#regarding copyright ownership.  The ASF licenses this file
#to you under the Apache License, Version 2.0 (the
#"License"); you may not use this file except in compliance
#with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing,
#software distributed under the License is distributed on an
#"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#KIND, either express or implied.  See the License for the
#specific language governing permissions and limitations
#under the License.

import subprocess
import urllib.request, urllib.parse, urllib.error
import hmac
import hashlib
import base64
import traceback
import logging
import re

from flask import Flask

app = Flask(__name__)

logger = logging.getLogger('baremetal-vr')
hdlr = logging.FileHandler('/var/log/baremetal-vr.log')
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
hdlr.setFormatter(formatter)
logger.addHandler(hdlr)
logger.setLevel(logging.WARNING)

class ShellCmd(object):
    '''
    classdocs
    '''
    def __init__(self, cmd, workdir=None, pipe=True):
        '''
        Constructor
        '''
        self.cmd = cmd
        if pipe:
            self.process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE, executable='/bin/sh', cwd=workdir)
        else:
            self.process = subprocess.Popen(cmd, shell=True, executable='/bin/sh', cwd=workdir)

        self.stdout = None
        self.stderr = None
        self.return_code = None

    def __call__(self, is_exception=True):
        (self.stdout, self.stderr) = self.process.communicate()
        if is_exception and self.process.returncode != 0:
            err = []
            err.append('failed to execute shell command: %s' % self.cmd)
            err.append('return code: %s' % self.process.returncode)
            err.append('stdout: %s' % self.stdout)
            err.append('stderr: %s' % self.stderr)
            raise Exception('\n'.join(err))

        self.return_code = self.process.returncode
        return self.stdout

def shell(cmd):
    return ShellCmd(cmd)()


class Server(object):
    CMDLINE = '/var/cache/cloud/cmdline'
    def __init__(self):
        self.apikey = None
        self.secretkey = None
        self.mgmtIp = None
        self.mgmtPort = None

    def _get_credentials(self):
        if not self.apikey or not self.secretkey:
            with open(self.CMDLINE, 'r') as fd:
                cmdline = fd.read()
                for p in cmdline.split():
                    if 'baremetalnotificationsecuritykey' in p:
                        self.secretkey = p.split("=")[1]
                    if 'baremetalnotificationapikey' in p:
                        self.apikey = p.split("=")[1]

        if not self.apikey:
            raise Exception('cannot find baremetalnotificationapikey in %s' % Server.CMDLINE)
        if not self.secretkey:
            raise Exception('cannot find baremetalnotificationsecuritykey in %s' % Server.CMDLINE)

        return self.apikey, self.secretkey

    def _get_mgmt_ip(self):
        if not self.mgmtIp:
            with open(self.CMDLINE, 'r') as fd:
                cmdline = fd.read()
                for p in cmdline.split():
                    if 'host' in p:
                        self.mgmtIp = p.split("=")[1]
                        break

        if not self.mgmtIp:
            raise Exception('cannot find host in %s' % Server.CMDLINE)

        return self.mgmtIp

    def _get_mgmt_port(self):
        if not self.mgmtPort:
            with open(self.CMDLINE, 'r') as fd:
                cmdline = fd.read()
                for p in cmdline.split():
                    if 'port' in p:
                        self.mgmtPort = p.split("=")[1]
                        break

        if not self.mgmtIp:
            raise Exception('cannot find port in %s' % Server.CMDLINE)

        return self.mgmtPort

    def _make_sign(self, mac):
        apikey, secretkey = self._get_credentials()
        reqs = {
            "apiKey": apikey,
            "command": 'notifyBaremetalProvisionDone',
            "mac": mac
        }

        request = list(zip(list(reqs.keys()), list(reqs.values())))
        request.sort(key=lambda x: str.lower(x[0]))
        hashStr = "&".join(["=".join([str.lower(r[0]), str.lower(urllib.parse.quote_plus(str(r[1]))).replace("+", "%20").replace('=', '%3d')]) for r in request])
        sig = urllib.parse.quote_plus(base64.encodebytes(hmac.new(secretkey, hashStr, hashlib.sha1).digest()).strip())
        return sig

    def notify_provisioning_done(self, mac):
        sig = self._make_sign(mac)
        cmd = 'http://%s:%s/client/api?command=notifyBaremetalProvisionDone&mac=%s&apiKey=%s&signature=%s' % (self._get_mgmt_ip(), self._get_mgmt_port(), mac, self.apikey, sig)
        shell("curl -X GET '%s'" % cmd)
        return ''

server = None

@app.route('/baremetal/provisiondone/<mac>', methods=['GET'])
def notify_provisioning_done(mac):
    try:
        if not is_a_mac(mac):
            raise "there is an issue with that '%s'. Not a mac?" % mac
        return server.notify_provisioning_done(mac)
    except:
        logger.warn(traceback.format_exc())
        return ''

def is_a_mac(mac):
    if re.match("[0-9a-f]{2}([-:]?)[0-9a-f]{2}(\\1[0-9a-f]{2}){4}$", mac.lower()):
        return True
    else:
        return False

if __name__ == '__main__':
    server = Server()
    shell("iptables-save | grep -- '-A INPUT -i eth0 -p tcp -m tcp --dport 10086 -j ACCEPT' > /dev/null || iptables -I INPUT -i eth0 -p tcp -m tcp --dport 10086 -j ACCEPT")
    app.run(host='0.0.0.0', port=10086)
