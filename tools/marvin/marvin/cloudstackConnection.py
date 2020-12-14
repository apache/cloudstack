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
import urllib.request, urllib.parse, urllib.error
import base64
import hmac
import hashlib
import time
from .cloudstackAPI import queryAsyncJobResult
from . import jsonHelper
from marvin.codes import (
    FAILED,
    JOB_FAILED,
    JOB_CANCELLED,
    JOB_SUCCEEDED
)
from marvin.cloudstackException import (
    InvalidParameterException,
    GetDetailExceptionInfo)


class CSConnection(object):

    '''
    @Desc: Connection Class to make API\Command calls to the
           CloudStack Management Server
           Sends the GET\POST requests to CS based upon the
           information provided and retrieves the parsed response.
    '''

    def __init__(self, mgmtDet, asyncTimeout=3600, logger=None,
                 path='client/api'):
        self.apiKey = mgmtDet.apiKey
        self.securityKey = mgmtDet.securityKey
        self.mgtSvr = mgmtDet.mgtSvrIp
        self.port = mgmtDet.port
        self.user = mgmtDet.user
        self.passwd = mgmtDet.passwd
        self.certPath = ()
        if mgmtDet.certCAPath != "NA" and mgmtDet.certPath != "NA":
            self.certPath = (mgmtDet.certCAPath, mgmtDet.certPath)
        self.logger = logger
        self.path = path
        self.retries = 5
        self.__lastError = ''
        self.mgtDetails = mgmtDet
        self.asyncTimeout = asyncTimeout
        self.auth = True
        if self.port == 8096 or \
           (self.apiKey is None and self.securityKey is None):
            self.auth = False
        self.protocol = "https" if mgmtDet.useHttps == "True" else "http"
        self.httpsFlag = True if self.protocol == "https" else False
        self.baseUrl = "%s://%s:%d/%s"\
                       % (self.protocol, self.mgtSvr, self.port, self.path)

    def __copy__(self):
        return CSConnection(self.mgtDetails,
                            self.asyncTimeout,
                            self.logger,
                            self.path)

    def __poll(self, jobid, response_cmd):
        '''
        @Name : __poll
        @Desc: polls for the completion of a given jobid
        @Input 1. jobid: Monitor the Jobid for CS
               2. response_cmd:response command for request cmd
        @return: FAILED if jobid is cancelled,failed
                 Else return async_response
        '''
        try:
            cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
            cmd.jobid = jobid
            timeout = self.asyncTimeout
            start_time = time.time()
            end_time = time.time()
            async_response = FAILED
            self.logger.debug("=== Jobid: %s Started ===" % (str(jobid)))
            while timeout > 0:
                async_response = self.\
                    marvinRequest(cmd, response_type=response_cmd)
                if async_response != FAILED:
                    job_status = async_response.jobstatus
                    if job_status in [JOB_CANCELLED,
                                      JOB_SUCCEEDED]:
                        break
                    elif job_status == JOB_FAILED:
                        raise Exception("Job failed: %s"\
                                         % async_response)
                time.sleep(1)
                timeout -= 1
                self.logger.debug("=== JobId:%s is Still Processing, "
                                  "Will TimeOut in:%s ====" % (str(jobid),
                                                               str(timeout)))
            end_time = time.time()
            tot_time = int(start_time - end_time)
            self.logger.debug(
                "===Jobid:%s ; StartTime:%s ; EndTime:%s ; "
                "TotalTime:%s===" %
                (str(jobid), str(time.ctime(start_time)),
                 str(time.ctime(end_time)), str(tot_time)))
            return async_response
        except Exception as e:
            self.__lastError = e
            self.logger.exception("==== __poll: Exception Occurred :%s ====" %
                                  str(self.__lastError))
            return FAILED

    def getLastError(self):
        '''
        @Name : getLastError
        @Desc : Returns the last error from marvinRequest
        '''
        return self.__lastError

    def __sign(self, payload):
        """
        @Name : __sign
        @Desc:signs a given request URL when the apiKey and
              secretKey are known
        @Input: payload: dictionary of params be signed
        @Output: the signature of the payload
        """
        params = list(zip(list(payload.keys()), list(payload.values())))
        params.sort(key=lambda k: str.lower(k[0]))
        hash_str = "&".join(
            ["=".join(
                [str.lower(r[0]),
                 str.lower(
                     urllib.parse.quote_plus(str(r[1]), safe="*")
                ).replace("+", "%20")]
            ) for r in params]
        )
        signature = base64.encodestring(hmac.new(
            self.securityKey, hash_str, hashlib.sha1).digest()).strip()
        return signature

    def __sendPostReqToCS(self, url, payload):
        '''
        @Name : __sendPostReqToCS
        @Desc : Sends the POST Request to CS
        @Input : url: URL to send post req
                 payload:Payload information as part of request
        @Output: Returns response from POST output
                 else FAILED
        '''
        try:
            response = requests.post(url,
                                     params=payload,
                                     cert=self.certPath,
                                     verify=self.httpsFlag)
            return response
        except Exception as e:
            self.__lastError = e
            self.logger.\
                exception("__sendPostReqToCS : Exception "
                          "Occurred: %s" % str(self.__lastError))
            return FAILED

    def __sendGetReqToCS(self, url, payload):
        '''
        @Name : __sendGetReqToCS
        @Desc : Sends the GET Request to CS
        @Input : url: URL to send post req
                 payload:Payload information as part of request
        @Output: Returns response from GET output
                 else FAILED
        '''
        try:
            response = requests.get(url,
                                    params=payload,
                                    cert=self.certPath,
                                    verify=self.httpsFlag)
            return response
        except Exception as e:
            self.__lastError = e
            self.logger.exception("__sendGetReqToCS : Exception Occurred: %s" %
                                  str(self.__lastError))
            return FAILED

    def __sendCmdToCS(self, command, auth=True, payload={}, method='GET'):
        """
        @Name : __sendCmdToCS
        @Desc : Makes requests to CS using the Inputs provided
        @Input: command: cloudstack API command name
                    eg: deployVirtualMachineCommand
                auth: Authentication (apikey,secretKey) => True
                     else False for integration.api.port
                payload: request data composed as a dictionary
                method: GET/POST via HTTP
        @output: FAILED or else response from CS
        """
        try:
            payload["command"] = command
            payload["response"] = "json"

            if auth:
                payload["apiKey"] = self.apiKey
                payload["signature"] = self.__sign(payload)

            # Verify whether protocol is "http" or "https", then send the
            # request
            if self.protocol in ["http", "https"]:
                self.logger.debug("Payload: %s" % str(payload))
                if method == 'POST':
                    self.logger.debug("=======Sending POST Cmd : %s======="
                                      % str(command))
                    return self.__sendPostReqToCS(self.baseUrl, payload)
                if method == "GET":
                    self.logger.debug("========Sending GET Cmd : %s======="
                                      % str(command))
                    return self.__sendGetReqToCS(self.baseUrl, payload)
            else:
                self.logger.exception("__sendCmdToCS: Invalid Protocol")
                return FAILED
        except Exception as e:
            self.__lastError = e
            self.logger.exception("__sendCmdToCS: Exception:%s" %
                                  GetDetailExceptionInfo(e))
            return FAILED

    def __sanitizeCmd(self, cmd):
        """
        @Name : __sanitizeCmd
        @Desc : Removes None values, Validates all required params are present
        @Input: cmd: Cmd object eg: createPhysicalNetwork
        @Output: Returns command name, asynchronous or not,request payload
                 FAILED for failed cases
        """
        try:
            cmd_name = ''
            payload = {}
            required = []
            isAsync = "false"
            for attribute in dir(cmd):
                if not attribute.startswith('__'):
                    if attribute == "isAsync":
                        isAsync = getattr(cmd, attribute)
                    elif attribute == "required":
                        required = getattr(cmd, attribute)
                    else:
                        payload[attribute] = getattr(cmd, attribute)
            cmd_name = cmd.__class__.__name__.replace("Cmd", "")
            for required_param in required:
                if payload[required_param] is None:
                    self.logger.debug("CmdName: %s Parameter : %s is Required"
                                      % (cmd_name, required_param))
                    self.__lastError = InvalidParameterException(
                        "Invalid Parameters")
                    return FAILED
            for param, value in list(payload.items()):
                if value is None:
                    payload.pop(param)
                elif param == 'typeInfo':
                    payload.pop(param)
                elif isinstance(value, list):
                    if len(value) == 0:
                        payload.pop(param)
                    else:
                        if not isinstance(value[0], dict):
                            payload[param] = ",".join(value)
                        else:
                            payload.pop(param)
                            i = 0
                            for val in value:
                                for k, v in val.items():
                                    payload["%s[%d].%s" % (param, i, k)] = v
                                i += 1
            return cmd_name.strip(), isAsync, payload
        except Exception as e:
            self.__lastError = e
            self.logger.\
                exception("__sanitizeCmd: CmdName : "
                          "%s : Exception:%s" % (cmd_name,
                                                 GetDetailExceptionInfo(e)))
            return FAILED

    def __parseAndGetResponse(self, cmd_response, response_cls, is_async):
        '''
        @Name : __parseAndGetResponse
        @Desc : Verifies the  Response(from CS) and returns an
                appropriate json parsed Response
        @Input: cmd_response: Command Response from cs
                response_cls : Mapping class for this Response
                is_async: Whether the cmd is async or not.
        @Output:Response output from CS
        '''
        try:
            try:
                ret = jsonHelper.getResultObj(
                    cmd_response.json(),
                    response_cls)
            except TypeError:
                ret = jsonHelper.getResultObj(cmd_response.json, response_cls)

            '''
            If the response is asynchronous, poll and return response
            else return response as it is
            '''
            if is_async == "false":
                self.logger.debug("Response : %s" % str(ret))
                return ret
            else:
                response = self.__poll(ret.jobid, response_cls)
                self.logger.debug("Response : %s" % str(response))
                return response.jobresult if response != FAILED else FAILED
        except Exception as e:
            self.__lastError = e
            self.logger.\
                exception("Exception:%s" % GetDetailExceptionInfo(e))
            return FAILED

    def marvinRequest(self, cmd, response_type=None, method='GET', data=''):
        """
        @Name : marvinRequest
        @Desc: Handles Marvin Requests
        @Input  cmd: marvin's command from cloudstackAPI
                response_type: response type of the command in cmd
                method: HTTP GET/POST, defaults to GET
        @Output: Response received from CS
                 Exception in case of Error\Exception
        """
        try:
            '''
            1. Verify the Inputs Provided
            '''
            if (cmd is None or cmd == ''):
                self.logger.exception("marvinRequest : Invalid Command Input")
                raise InvalidParameterException("Invalid Parameter")

            '''
            2. Sanitize the Command
            '''
            sanitize_cmd_out = self.__sanitizeCmd(cmd)

            if sanitize_cmd_out == FAILED:
                raise self.__lastError

            cmd_name, is_async, payload = sanitize_cmd_out
            '''
            3. Send Command to CS
            '''
            cmd_response = self.__sendCmdToCS(cmd_name,
                                              self.auth,
                                              payload=payload,
                                              method=method)
            if cmd_response == FAILED:
                raise self.__lastError

            '''
            4. Check if the Command Response received above is valid or Not.
               If not return Invalid Response
            '''
            ret = self.__parseAndGetResponse(cmd_response,
                                             response_type,
                                             is_async)
            if ret == FAILED:
                raise self.__lastError
            return ret
        except Exception as e:
            self.logger.exception("marvinRequest : CmdName: %s Exception: %s" %
                                  (str(cmd), GetDetailExceptionInfo(e)))
            raise e
