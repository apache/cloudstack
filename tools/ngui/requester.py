#!/usr/bin/python
# -*- coding: utf-8 -*-
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

try:
    import base64
    import hashlib
    import hmac
    import httplib
    import json
    import os
    import pdb
    import re
    import shlex
    import sys
    import time
    import types
    import urllib
    import urllib2

except ImportError, e:
    print "Import error in %s : %s" % (__name__, e)
    import sys
    sys.exit()


def logger_debug(logger, message):
    if logger is not None:
        logger.debug(message)


def make_request(command, args, logger, host, port,
                 apikey, secretkey, protocol, path):
    response = None
    error = None

    if protocol != 'http' and protocol != 'https':
        error = "Protocol must be 'http' or 'https'"
        return None, error

    if args is None:
        args = {}

    args["command"] = command
    args["apiKey"] = apikey
    args["response"] = "json"
    request = zip(args.keys(), args.values())
    request.sort(key=lambda x: x[0].lower())

    request_url = "&".join(["=".join([r[0], urllib.quote_plus(str(r[1]))])
                           for r in request])
    hashStr = "&".join(["=".join([r[0].lower(),
                       str.lower(urllib.quote_plus(str(r[1]))).replace("+",
                       "%20")]) for r in request])

    sig = urllib.quote_plus(base64.encodebytes(hmac.new(secretkey, hashStr,
                            hashlib.sha1).digest()).strip())
    request_url += "&signature=%s" % sig
    request_url = "%s://%s:%s%s?%s" % (protocol, host, port, path, request_url)

    try:
        logger_debug(logger, "Request sent: %s" % request_url)
        connection = urllib2.urlopen(request_url)
        response = connection.read()
    except Exception, e:
        error = str(e)

    logger_debug(logger, "Response received: %s" % response)
    if error is not None:
        logger_debug(logger, error)

    return response, error


def monkeyrequest(command, args, isasync, asyncblock, logger, host, port,
                  apikey, secretkey, timeout, protocol, path):

    response = None
    error = None
    logger_debug(logger, "======== START Request ========")
    logger_debug(logger, "Requesting command=%s, args=%s" % (command, args))
    response, error = make_request(command, args, logger, host, port,
                                   apikey, secretkey, protocol, path)
    logger_debug(logger, "======== END Request ========\n")

    if error is not None:
        return response, error

    def process_json(response):
        try:
            response = json.loads(str(response))
        except ValueError, e:
            error = "Error processing json response, %s" % e
            logger_debug(logger, "Error processing json", e)
        return response

    response = process_json(response)
    if response is None:
        return response, error

    isasync = isasync and (asyncblock == "true")
    responsekey = filter(lambda x: 'response' in x, response.keys())[0]

    if isasync and 'jobid' in response[responsekey]:
        jobid = response[responsekey]['jobid']
        command = "queryAsyncJobResult"
        request = {'jobid': jobid}
        timeout = int(timeout)
        pollperiod = 3
        progress = 1
        while timeout > 0:
            print '\r' + '.' * progress,
            time.sleep(pollperiod)
            timeout = timeout - pollperiod
            progress += 1
            logger_debug(logger, "Job %s to timeout in %ds" % (jobid, timeout))
            sys.stdout.flush()
            response, error = monkeyrequest(command, request, isasync,
                                            asyncblock, logger,
                                            host, port,  apikey, secretkey,
                                            timeout, protocol, path)
            response = process_json(response)
            responsekeys = filter(lambda x: 'response' in x, response.keys())
            if len(responsekeys) < 1:
                continue
            result = response[responsekeys[0]]
            jobstatus = result['jobstatus']
            if jobstatus == 2:
                jobresult = result["jobresult"]
                error = "\rAsync job %s failed\nError %s, %s" % (jobid,
                        jobresult["errorcode"], jobresult["errortext"])
                return response, error
            elif jobstatus == 1:
                print '\r',
                return response, error
        error = "Error: Async query timeout occurred for jobid %s" % jobid

    return response, error
