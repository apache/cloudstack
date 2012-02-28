import urllib2
import urllib
import base64
import copy
import hmac
import hashlib
import json
import xml.dom.minidom
import types
import time
import inspect
import cloudstackException
from cloudstackAPI import * 
import jsonHelper

class cloudConnection(object):
    def __init__(self, mgtSvr, port=8096, apiKey = None, securityKey = None, asyncTimeout=3600, logging=None):
        self.apiKey = apiKey
        self.securityKey = securityKey
        self.mgtSvr = mgtSvr
        self.port = port
        self.logging = logging
        if port == 8096:
            self.auth = False
        else:
            self.auth = True
            
        self.retries = 5
        self.asyncTimeout = asyncTimeout
    
    def close(self):
        try:
            self.connection.close()
        except:
            pass
    
    def __copy__(self):
        return cloudConnection(self.mgtSvr, self.port, self.apiKey, self.securityKey, self.asyncTimeout, self.logging)
    
    def make_request_with_auth(self, command, requests={}):
        requests["command"] = command
        requests["apiKey"] = self.apiKey
        requests["response"] = "json"
        request = zip(requests.keys(), requests.values())
        request.sort(key=lambda x: str.lower(x[0]))
        
        requestUrl = "&".join(["=".join([r[0], urllib.quote_plus(str(r[1]))]) for r in request])
        hashStr = "&".join(["=".join([str.lower(r[0]), str.lower(urllib.quote_plus(str(r[1]))).replace("+", "%20")]) for r in request])

        sig = urllib.quote_plus(base64.encodestring(hmac.new(self.securityKey, hashStr, hashlib.sha1).digest()).strip())
        requestUrl += "&signature=%s"%sig

        try:
            self.connection = urllib2.urlopen("http://%s:%d/client/api?%s"%(self.mgtSvr, self.port, requestUrl))
            self.logging.debug("sending request: %s"%requestUrl)
            response = self.connection.read()
            self.logging.debug("got response: %s"%response)
        except IOError, e:
            if hasattr(e, 'reason'):
                self.logging.debug("failed to reach %s because of %s"%(self.mgtSvr, e.reason))
            elif hasattr(e, 'code'):
                self.logging.debug("server returned %d error code"%e.code)
        except HTTPException, h:
            self.logging.debug("encountered http Exception %s"%h.args)
            if self.retries > 0:
                self.retries = self.retries - 1
                self.make_request_with_auth(command, requests)
            else:
                self.retries = 5
                raise h
        else:
            return response
        
    def make_request_without_auth(self, command, requests={}):
        requests["command"] = command
        requests["response"] = "json" 
        requests = zip(requests.keys(), requests.values())
        requestUrl = "&".join(["=".join([request[0], urllib.quote_plus(str(request[1]))]) for request in requests])

        self.connection = urllib2.urlopen("http://%s:%d/client/api?%s"%(self.mgtSvr, self.port, requestUrl))
        self.logging.debug("sending request without auth: %s"%requestUrl)
        response = self.connection.read()
        self.logging.debug("got response: %s"%response)
        return response
    
    def pollAsyncJob(self, jobId, response):
        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = jobId
        
        while self.asyncTimeout > 0:
            asyncResonse = self.make_request(cmd, response, True)
            
            if asyncResonse.jobstatus == 2:
                raise cloudstackException.cloudstackAPIException("asyncquery", asyncResonse.jobresult)
            elif asyncResonse.jobstatus == 1:
                return asyncResonse
            
            time.sleep(5)
            self.asyncTimeout = self.asyncTimeout - 5
            
        raise cloudstackException.cloudstackAPIException("asyncquery", "Async job timeout")
    
    def make_request(self, cmd, response = None, raw=False):
        commandName = cmd.__class__.__name__.replace("Cmd", "")
        isAsync = "false"
        requests = {}
        required = []
        for attribute in dir(cmd):
            if attribute != "__doc__" and attribute != "__init__" and attribute != "__module__":
                if attribute == "isAsync":
                    isAsync = getattr(cmd, attribute)
                elif attribute == "required":
                    required = getattr(cmd, attribute)
                else:
                    requests[attribute] = getattr(cmd, attribute)
        
        for requiredPara in required:
            if requests[requiredPara] is None:
                raise cloudstackException.cloudstackAPIException(commandName, "%s is required"%requiredPara)
        '''remove none value'''
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
                        for v in value:
                            for key, val in v.iteritems():
                                requests["%s[%d].%s"%(param,i,key)] = val
                            i = i + 1
        
        if self.logging is not None:
            self.logging.debug("sending command: %s %s"%(commandName, str(requests)))
        result = None
        if self.auth:
            result = self.make_request_with_auth(commandName, requests)
        else:
            result = self.make_request_without_auth(commandName, requests)
        
        if result is None:
            return None
        if self.logging is not None:
            self.logging.debug("got result: "  + result)
        
        result = jsonHelper.getResultObj(result, response)
        if raw or isAsync == "false":
            return result
        else:
            asynJobId = result.jobid
            result = self.pollAsyncJob(asynJobId, response)
            return result.jobresult
        
if __name__ == '__main__':
    xml = '<?xml version="1.0" encoding="ISO-8859-1"?><deployVirtualMachineResponse><virtualmachine><id>407</id><name>i-1-407-RS3</name><displayname>i-1-407-RS3</displayname><account>system</account><domainid>1</domainid><domain>ROOT</domain><created>2011-07-30T14:45:19-0700</created><state>Running</state><haenable>false</haenable><zoneid>1</zoneid><zonename>CA1</zonename><hostid>3</hostid><hostname>kvm-50-205</hostname><templateid>4</templateid><templatename>CentOS 5.5(64-bit) no GUI (KVM)</templatename><templatedisplaytext>CentOS 5.5(64-bit) no GUI (KVM)</templatedisplaytext><passwordenabled>false</passwordenabled><serviceofferingid>1</serviceofferingid><serviceofferingname>Small Instance</serviceofferingname><cpunumber>1</cpunumber><cpuspeed>500</cpuspeed><memory>512</memory><guestosid>112</guestosid><rootdeviceid>0</rootdeviceid><rootdevicetype>NetworkFilesystem</rootdevicetype><nic><id>380</id><networkid>203</networkid><netmask>255.255.255.0</netmask><gateway>65.19.181.1</gateway><ipaddress>65.19.181.110</ipaddress><isolationuri>vlan://65</isolationuri><broadcasturi>vlan://65</broadcasturi><traffictype>Guest</traffictype><type>Direct</type><isdefault>true</isdefault><macaddress>06:52:da:00:00:08</macaddress></nic><hypervisor>KVM</hypervisor></virtualmachine></deployVirtualMachineResponse>'
    conn = cloudConnection(None)
    
    print conn.paraseReturnXML(xml, deployVirtualMachine.deployVirtualMachineResponse())
