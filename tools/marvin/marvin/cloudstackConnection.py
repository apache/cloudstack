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

import requests
import urllib
import base64
import hmac
import hashlib
import logging
import time
import cloudstackException
from cloudstackAPI import *
import jsonHelper
from requests import ConnectionError
from requests import HTTPError
from requests import Timeout
from requests import RequestException


class cloudConnection(object):

    """ Connections to make API calls to the cloudstack management server
    """
    def __init__(self, mgmtDet, asyncTimeout=3600, logging=None,
                 scheme='http', path='client/api'):
        self.loglevel()  # Turn off requests logs
        self.apiKey = mgmtDet.apiKey
        self.securityKey = mgmtDet.securityKey
        self.mgtSvr = mgmtDet.mgtSvrIp
        self.port = mgmtDet.port
        self.user = mgmtDet.user
        self.passwd = mgmtDet.passwd
        self.certCAPath = mgmtDet.certCAPath
        self.certPath = mgmtDet.certPath
        self.logging = logging
        self.path = path
        self.retries = 5
        self.protocol = scheme
        self.asyncTimeout = asyncTimeout
        self.auth = True
        if self.port == 8096 or \
           (self.apiKey is None and self.securityKey is None):
            self.auth = False
        if mgmtDet.useHttps == "True":
            self.protocol = "https"
        self.baseurl = "%s://%s:%d/%s"\
                       % (self.protocol, self.mgtSvr, self.port, self.path)

    def __copy__(self):
        return cloudConnection(self.mgtSvr, self.port, self.user,
                               self.passwd, self.apiKey,
                               self.securityKey,
                               self.asyncTimeout, self.logging,
                               self.protocol,
                               self.path)

    def loglevel(self, lvl=logging.WARNING):
        """
        Turns off the INFO/DEBUG logs from `requests`
        """
        requests_log = logging.getLogger("requests")
        requests_log.setLevel(lvl)

    def poll(self, jobid, response):
        """
        polls the completion of a given jobid
        @param jobid:
        @param response:
        @return:
        """
        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = jobid
        timeout = self.asyncTimeout

        while timeout > 0:
            asyncResonse = self.marvinRequest(cmd, response_type=response)

            if asyncResonse.jobstatus == 2:
                raise cloudstackException.cloudstackAPIException(
                    "asyncquery", asyncResonse.jobresult)
            elif asyncResonse.jobstatus == 1:
                return asyncResonse

            time.sleep(5)
            if self.logging is not None:
                self.logging.debug("job: %s still processing,"
                                   " will timeout in %ds" % (jobid, timeout))
            timeout = timeout - 5

        raise cloudstackException.cloudstackAPIException(
            "asyncquery", "Async job timeout %s" % jobid)

    def sign(self, payload):
        """
        signs a given request URL when the apiKey and secretKey are known

        @param payload: dict of GET params to be signed
        @return: the signature of the payload
        """
        params = zip(payload.keys(), payload.values())
        params.sort(key=lambda k: str.lower(k[0]))
        hashStr = "&".join(
            ["=".join(
                [str.lower(r[0]),
                 str.lower(
                     urllib.quote_plus(str(r[1]))
                 ).replace("+", "%20")]
            ) for r in params]
        )
        signature = base64.encodestring(hmac.new(
            self.securityKey, hashStr, hashlib.sha1).digest()).strip()
        self.logging.debug("Computed Signature by Marvin: %s" % signature)
        return signature

    def request(self, command, auth=True, payload={}, method='GET'):
        """
        Makes requests using auth or over integration port
        @param command: cloudstack API command name
                    eg: deployVirtualMachineCommand
        @param auth: Authentication (apikey,secretKey) => True
                     else False for integration.api.port
        @param payload: request data composed as a dictionary
        @param method: GET/POST via HTTP
        @return:
        """
        payload["command"] = command
        payload["response"] = "json"

        if auth:
            payload["apiKey"] = self.apiKey
            signature = self.sign(payload)
            payload["signature"] = signature

        try:
            #https_flag : Signifies whether to verify connection over \
            #http or https, \
            #initialized to False, will be set to true if user provided https
            #connection
            https_flag = False
            cert_path = ()
            if self.protocol == "https":
                https_flag = True
                if self.certCAPath != "NA" and self.certPath != "NA":
                    cert_path = (self.certCAPath, self.certPath)

            #Verify whether protocol is "http", then call the request over http
            if self.protocol == "http":
                if method == 'POST':
                    response = requests.post(self.baseurl, params=payload,
                                             verify=https_flag)
                else:
                    response = requests.get(self.baseurl, params=payload,
                                            verify=https_flag)
            else:
                '''
                If protocol is https, then create the  connection url with \
                user provided certificates \
                provided as part of cert
                '''
                try:
                    if method == 'POST':
                        response = requests.post(self.baseurl,
                                                 params=payload,
                                                 cert=cert_path,
                                                 verify=https_flag)
                    else:
                        response = requests.get(self.baseurl, params=payload,
                                                cert=cert_path,
                                                verify=https_flag)
                except Exception, e:
                    '''
                    If an exception occurs with user provided CA certs, \
                    then try with default certs, \
                    we dont need to mention here the cert path
                    '''
                    self.logging.debug("Creating CS connection over https \
                                        didnt worked with user provided certs \
                                            , so trying with no certs %s" % e)
                    if method == 'POST':
                        response = requests.post(self.baseurl,
                                                 params=payload,
                                                 verify=https_flag)
                    else:
                        response = requests.get(self.baseurl,
                                                params=payload,
                                                verify=https_flag)
        except ConnectionError, c:
            self.logging.debug("Connection refused. Reason: %s : %s"
                               % (self.baseurl, c))
            raise c
        except HTTPError, h:
            self.logging.debug("Http Error.Server returned error code: %s" % h)
            raise h
        except Timeout, t:
            self.logging.debug("Connection timed out with %s" % t)
            raise t
        except RequestException, r:
            self.logging.debug("RequestException from server %s" % r)
            raise r
        except Exception, e:
            self.logging.debug("Error returned by server %s" % r)
            raise e
        else:
            return response

    def sanitizeCommand(self, cmd):
        """
        Removes None values, Validates all required params are present
        @param cmd: Cmd object eg: createPhysicalNetwork
        @return:
        """
        requests = {}
        required = []
        for attribute in dir(cmd):
            if not attribute.startswith('__'):
                if attribute == "isAsync":
                    isAsync = getattr(cmd, attribute)
                elif attribute == "required":
                    required = getattr(cmd, attribute)
                else:
                    requests[attribute] = getattr(cmd, attribute)

        cmdname = cmd.__class__.__name__.replace("Cmd", "")
        for requiredPara in required:
            if requests[requiredPara] is None:
                raise cloudstackException.cloudstackAPIException(
                    cmdname, "%s is required" % requiredPara)
        for param, value in requests.items():
            if value is None:
                requests.pop(param)
            elif isinstance(value, list):
                if len(value) == 0:
                    requests.pop(param)
                else:
                    if not isinstance(value[0], dict):
                        requests[param] = ",".join(value)
                    else:
                        requests.pop(param)
                        i = 0
                        for val in value:
                            for k, v in val.iteritems():
                                requests["%s[%d].%s" % (param, i, k)] = v
                            i = i + 1
        return cmdname.strip(), isAsync, requests

    def marvinRequest(self, cmd, response_type=None, method='GET', data=''):
        """
        Requester for marvin command objects
        @param cmd: marvin's command from cloudstackAPI
        @param response_type: response type of the command in cmd
        @param method: HTTP GET/POST, defaults to GET
        @return:
        """
        cmdname, isAsync, payload = self.sanitizeCommand(cmd)
        self.logging.debug("sending %s request: %s %s" % (method, cmdname,
                                                          str(payload)))
        response = self.request(cmdname,
                                self.auth,
                                payload=payload,
                                method=method)
        self.logging.debug("Request: %s Response: %s" %
                           (response.url, response.text))
        try:
            response = jsonHelper.getResultObj(response.json(), response_type)
        except TypeError:
            response = jsonHelper.getResultObj(response.json, response_type)

        if isAsync == "false":
            return response
        else:
            asyncJobId = response.jobid
            response = self.poll(asyncJobId, response_type)
            return response.jobresult
