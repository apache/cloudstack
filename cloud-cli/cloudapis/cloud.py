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



'''Implements the CloudStack API'''


from cloudtool.utils import describe
import urllib
import urllib2
import os
import xml.dom.minidom
import re
import base64
import hmac
import hashlib
import httplib

class CloudAPI:

    @describe("server", "Management Server host name or address")
    @describe("apikey", "Management Server apiKey")
    @describe("securitykey", "Management Server securityKey")
    @describe("responseformat", "Response format: xml or json")
    @describe("stripxml", "True if xml tags have to be stripped in the output, false otherwise")
    def __init__(self,
            server="127.0.0.1:8096",
            responseformat="xml",
            stripxml="true",
            apiKey=None,
            securityKey=None
            ):
        self.__dict__.update(locals())

    def _make_request_with_keys(self,command,requests={}):
        requests["command"] = command
        requests["apiKey"] = self.apiKey
        requests["response"] = "xml"
        requests = zip(requests.keys(), requests.values())
        requests.sort(key=lambda x: str.lower(x[0]))

        requestUrl = "&".join(["=".join([request[0], urllib.quote_plus(str(request[1]))]) for request in requests])
        hashStr = "&".join(["=".join([str.lower(request[0]), urllib.quote_plus(str.lower(str(request[1])))]) for request in requests])

        sig = urllib.quote_plus(base64.encodestring(hmac.new(self.securityKey, hashStr, hashlib.sha1).digest()).strip())

        requestUrl += "&signature=%s"%sig
        return requestUrl


    def _make_request_with_auth(self, command, requests):
        self.connection = httplib.HTTPConnection("%s"%(self.server))
        requests["command"] = command
        requests["apiKey"] = self.apiKey
        requests["response"] = self.responseformat
        requests = zip(requests.keys(), requests.values())
        requests.sort(key=lambda x: str.lower(x[0]))

        requestUrl = "&".join(["=".join([request[0], urllib.quote(str(request[1],""))]) for request in requests])
        hashStr = "&".join(["=".join([str.lower(request[0]), urllib.quote(str.lower(str(request[1])),"")]) for request in requests])

        sig = urllib.quote_plus(base64.encodestring(hmac.new(self.securityKey, str.lower(hashStr), hashlib.sha1).digest()).strip())

        requestUrl += "&signature=%s"%sig

        self.connection.request("GET", "/client/api?%s"%requestUrl)
        return self.connection.getresponse().read()

    def _make_request(self,command,parameters=None):

        '''Command is a string, parameters is a dictionary'''
        if ":" in self.server:
            host,port = self.server.split(":")
            port = int(port)
        else:
            host = self.server
            port = 8096

        url = "http://" + self.server + "/client/api?"

        if not parameters: parameters = {}
        if self.apiKey is not None and self.securityKey is not None:
            return self._make_request_with_auth(command, parameters)
        else:
            parameters["command"] = command
            parameters["response"] = self.responseformat
            querystring = urllib.urlencode(parameters)

        url += querystring

        f = urllib2.urlopen(url)
        data = f.read()
        if self.stripxml == "true":
            data=re.sub("<\?.*\?>", "\n", data);
            data=re.sub("</[a-z]*>", "\n", data);
            data=data.replace(">", "=");
            data=data.replace("=<", "\n");
            data=data.replace("\n<", "\n");
            data=re.sub("\n.*cloud-stack-version=.*", "", data);
            data=data.replace("\n\n\n", "\n");

        return data


def load_dynamic_methods():
    '''creates smart function objects for every method in the commands.xml file'''

    def getText(nodelist):
        rc = []
        for node in nodelist:
            if node.nodeType == node.TEXT_NODE: rc.append(node.data)
        return ''.join(rc)

    # FIXME figure out installation and packaging
    xmlfile = os.path.join("/etc/cloud/cli/","commands.xml")
    dom = xml.dom.minidom.parse(xmlfile)

    for cmd in dom.getElementsByTagName("command"):
        name = getText(cmd.getElementsByTagName('name')[0].childNodes).strip()
        assert name

        description = getText(cmd.getElementsByTagName('description')[0].childNodes).strip()
        if description:
                    description = '"""%s"""' % description
        else: description = ''
        arguments = []
        options = []
        descriptions = []

        for param in cmd.getElementsByTagName("request")[0].getElementsByTagName("arg"):
            argname = getText(param.getElementsByTagName('name')[0].childNodes).strip()
            assert argname

            required = getText(param.getElementsByTagName('required')[0].childNodes).strip()
            if required == 'true': required = True
            elif required == 'false': required = False
            else: raise AssertionError, "Not reached"
            if required: arguments.append(argname)
            options.append(argname)

                        #import ipdb; ipdb.set_trace()
            requestDescription = param.getElementsByTagName('description')
            if requestDescription:
                descriptionParam = getText(requestDescription[0].childNodes)
            else:
                descriptionParam = ''
            if descriptionParam: descriptions.append( (argname,descriptionParam) )

        funcparams = ["self"] + [ "%s=None"%o for o in options ]
        funcparams = ", ".join(funcparams)

        code = """
        def %s(%s):
            %s
            parms = dict(locals())
            del parms["self"]
            for arg in %r:
                if locals()[arg] is None:
                    raise TypeError, "%%s is a required option"%%arg
            for k,v in parms.items():
                if v is None: del parms[k]
            output = self._make_request("%s",parms)
            return output
        """%(name,funcparams,description,arguments,name)

        namespace = {}
        exec code.strip() in namespace

        func = namespace[name]
        for argname,description in descriptions:
            func = describe(argname,description)(func)

        yield (name,func)


for name,meth in load_dynamic_methods():
    setattr(CloudAPI, name, meth)

implementor = CloudAPI

del name,meth,describe,load_dynamic_methods


