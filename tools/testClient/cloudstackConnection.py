import httplib
import urllib
import base64
import hmac
import hashlib
import json
import xml.dom.minidom
import types
import time
import inspect
import cloudstackException
from cloudstackAPI import * 

class cloudConnection(object):
    def __init__(self, mgtSvr, port=8096, apiKey = None, securityKey = None, asyncTimeout=3600, logging=None):
        self.apiKey = apiKey
        self.securityKey = securityKey
        self.mgtSvr = mgtSvr
        self.connection = httplib.HTTPConnection("%s:%d"%(mgtSvr,port))
        self.port = port
        self.logging = logging
        if port == 8096:
            self.auth = False
        else:
            self.auth = True
            
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
        requests["response"] = "xml"
        requests = zip(requests.keys(), requests.values())
        requests.sort(key=lambda x: str.lower(x[0]))
        
        requestUrl = "&".join(["=".join([request[0], urllib.quote_plus(str(request[1]))]) for request in requests])
        hashStr = "&".join(["=".join([str.lower(request[0]), urllib.quote_plus(str.lower(str(request[1])))]) for request in requests])

        sig = urllib.quote_plus(base64.encodestring(hmac.new(self.securityKey, hashStr, hashlib.sha1).digest()).strip())

        requestUrl += "&signature=%s"%sig

        self.connection.request("GET", "/client/api?%s"%requestUrl)
        return self.connection.getresponse().read()
 
    def make_request_without_auth(self, command, requests={}):
        requests["command"] = command
        requests["response"] = "xml" 
        requests = zip(requests.keys(), requests.values())
        requestUrl = "&".join(["=".join([request[0], urllib.quote_plus(str(request[1]))]) for request in requests])
        self.connection.request("GET", "/&%s"%requestUrl)
        return self.connection.getresponse().read()
    
    def getText(self, elements):
        if len(elements) < 1:
            return None
        if not elements[0].hasChildNodes():
            return None
        if elements[0].childNodes[0].nodeValue is None:
            return None
        return elements[0].childNodes[0].nodeValue.strip()
    
    def getclassFromName(self, cmd, name):
        module = inspect.getmodule(cmd)
        return getattr(module, name)()
    def parseOneInstance(self, element, instance):
        ItemsNeedToCheck = {}
        for attribute in dir(instance):
            if attribute != "__doc__" and attribute != "__init__" and attribute != "__module__":
                ItemsNeedToCheck[attribute] = getattr(instance, attribute)
        for attribute, value in ItemsNeedToCheck.items():
            if type(value) == types.ListType:
                subItem = []
                for subElement in element.getElementsByTagName(attribute):
                    newInstance = self.getclassFromName(instance, attribute)
                    self.parseOneInstance(subElement, newInstance)
                    subItem.append(newInstance)
                setattr(instance, attribute, subItem)
                continue
            else:
                item = element.getElementsByTagName(attribute)
                if len(item) == 0:
                    continue
                
                returnValue = self.getText(item)
                setattr(instance, attribute, returnValue)
        
    def hasErrorCode(self, elements, responseName):
        errorCode = elements[0].getElementsByTagName("errorcode")
        if len(errorCode) > 0:
            erroCodeText = self.getText(errorCode)
            errorText = elements[0].getElementsByTagName("errortext")
            if len(errorText) > 0:
                errorText = self.getText(errorText)
            errMsg = "errorCode: %s, errorText:%s"%(erroCodeText, errorText)
            raise cloudstackException.cloudstackAPIException(responseName, errMsg)
    
    def paraseReturnXML(self, result, response):
        responseName = response.__class__.__name__.lower()
        cls = response.__class__
       
        responseLists = []
        morethanOne = False
        
        dom = xml.dom.minidom.parseString(result)
        elements = dom.getElementsByTagName(responseName)
        if len(elements) == 0:
            return responseLists
        
        self.hasErrorCode(elements, responseName)
        
        count = elements[0].getElementsByTagName("count")
        if len(count) > 0:
            morethanOne = True
            for node in elements[0].childNodes:
                if node.nodeName == "count":
                    continue
                newInstance = cls()
                self.parseOneInstance(node, newInstance)
                responseLists.append(newInstance)
                
        else:
            if elements[0].hasChildNodes():
                newInstance = cls()
                self.parseOneInstance(elements[0], newInstance)
                responseLists.append(newInstance)
        
        return responseLists, morethanOne
    
    def paraseResultFromElement(self, elements, response):
        responseName = response.__class__.__name__.lower()
        cls = response.__class__
      
        responseLists = []
        morethanOne = False
      
        newInstance = cls()
        self.parseOneInstance(elements[0], newInstance)
        responseLists.append(newInstance)
        
        return responseLists, morethanOne
    def getAsyncJobId(self, response, resultXml):
        responseName = response.__class__.__name__.lower()
        dom = xml.dom.minidom.parseString(resultXml)
        elements = dom.getElementsByTagName(responseName)
        if len(elements) == 0:
            raise cloudstackException.cloudstackAPIException("can't find %s"%responseName)
        
        self.hasErrorCode(elements, responseName)
        
        jobIdEle = elements[0].getElementsByTagName("jobid")
        if len(jobIdEle) == 0:
            errMsg = 'can not find jobId in the result:%s'%resultXml
            
            raise cloudstackException.cloudstackAPIException(errMsg)
        
        jobId = self.getText(jobIdEle)
        return jobId
    
    def pollAsyncJob(self, cmd, response, jobId):
        commandName = cmd.__class__.__name__.replace("Cmd", "")
        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = jobId
        
        while self.asyncTimeout > 0:
            asyncResponse = queryAsyncJobResult.queryAsyncJobResultResponse()
            responseName = asyncResponse.__class__.__name__.lower()
            asyncResponseXml = self.make_request(cmd, asyncResponse, True)
            dom = xml.dom.minidom.parseString(asyncResponseXml)
            elements = dom.getElementsByTagName(responseName)
            if len(elements) == 0:
                raise cloudstackException.cloudstackAPIException("can't find %s"%responseName)
        
            self.hasErrorCode(elements, responseName)
            
            jobstatus =  self.getText(elements[0].getElementsByTagName("jobstatus"))
            
            if jobstatus == "2":
                jobResult = self.getText(elements[0].getElementsByTagName("jobresult"))
                raise cloudstackException.cloudstackAPIException(commandName, jobResult)
            elif jobstatus == "1":
                jobResultEle = elements[0].getElementsByTagName("jobresult")
                
                return self.paraseResultFromElement(jobResultEle, response)
                
            time.sleep(5)
            
        raise cloudstackException.cloudstackAPIException(commandName, "Async job timeout")
    def make_request(self, cmd, response, raw=False):
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
        if self.logging is not None:
            self.logging.debug("sending command: " + str(requests))
        result = None
        if self.auth:
            result = self.make_request_with_auth(commandName, requests)
        else:
            result = self.make_request_without_auth(commandName, requests)
        
        if self.logging is not None:
            self.logging.debug("got result: "  + result)
        if result is None:
            return None
        
        if raw:
            return result
        if isAsync == "false":
            result,num = self.paraseReturnXML(result, response)
        else:
            jobId = self.getAsyncJobId(response, result)
            result,num = self.pollAsyncJob(cmd, response, jobId)
        if num:
            return result
        else:
            if len(result) != 0:
               return result[0]
            return None
        
if __name__ == '__main__':
    xml = '<?xml version="1.0" encoding="ISO-8859-1"?><deployVirtualMachineResponse><virtualmachine><id>407</id><name>i-1-407-RS3</name><displayname>i-1-407-RS3</displayname><account>system</account><domainid>1</domainid><domain>ROOT</domain><created>2011-07-30T14:45:19-0700</created><state>Running</state><haenable>false</haenable><zoneid>1</zoneid><zonename>CA1</zonename><hostid>3</hostid><hostname>kvm-50-205</hostname><templateid>4</templateid><templatename>CentOS 5.5(64-bit) no GUI (KVM)</templatename><templatedisplaytext>CentOS 5.5(64-bit) no GUI (KVM)</templatedisplaytext><passwordenabled>false</passwordenabled><serviceofferingid>1</serviceofferingid><serviceofferingname>Small Instance</serviceofferingname><cpunumber>1</cpunumber><cpuspeed>500</cpuspeed><memory>512</memory><guestosid>112</guestosid><rootdeviceid>0</rootdeviceid><rootdevicetype>NetworkFilesystem</rootdevicetype><nic><id>380</id><networkid>203</networkid><netmask>255.255.255.0</netmask><gateway>65.19.181.1</gateway><ipaddress>65.19.181.110</ipaddress><isolationuri>vlan://65</isolationuri><broadcasturi>vlan://65</broadcasturi><traffictype>Guest</traffictype><type>Direct</type><isdefault>true</isdefault><macaddress>06:52:da:00:00:08</macaddress></nic><hypervisor>KVM</hypervisor></virtualmachine></deployVirtualMachineResponse>'
    conn = cloudConnection(None)
    
    print conn.paraseReturnXML(xml, deployVirtualMachine.deployVirtualMachineResponse())