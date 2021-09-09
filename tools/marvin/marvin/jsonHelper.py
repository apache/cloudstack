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
from . import cloudstackException
import json
import inspect
from marvin.cloudstackAPI import *


class jsonLoader(object):

    '''The recursive class for building and representing objects with.'''

    def __init__(self, obj):
        for k in obj:
            v = obj[k]
            if isinstance(v, dict):
                setattr(self, k, jsonLoader(v))
            elif isinstance(v, (list, tuple)):
                if len(v) > 0 and isinstance(v[0], dict):
                    setattr(self, k, [jsonLoader(elem) for elem in v])
                else:
                    setattr(self, k, v)
            else:
                setattr(self, k, v)

    def __getattr__(self, val):
        if val in self.__dict__:
            return self.__dict__[val]
        else:
            return None

    def __getitem__(self, val):
        if val in self.__dict__:
            return self.__dict__[val]
        else:
            return None

    def __repr__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v)
                                      in self.__dict__.items()))

    def __str__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v)
                                      in self.__dict__.items()))


class jsonDump(object):

    @staticmethod
    def __serialize(obj):
        """Recursively walk object's hierarchy."""
        if isinstance(obj, (bool, int, float, str)):
            return obj
        elif isinstance(obj, dict):
            obj = obj.copy()
            newobj = {}
            for key in obj:
                if obj[key] is not None:
                    if (isinstance(obj[key], list) and len(obj[key]) == 0):
                        continue
                    newobj[key] = jsonDump.__serialize(obj[key])

            return newobj
        elif isinstance(obj, list):
            return [jsonDump.__serialize(item) for item in obj]
        elif isinstance(obj, tuple):
            return tuple(jsonDump.__serialize([item for item in obj]))
        elif hasattr(obj, '__dict__'):
            return jsonDump.__serialize(obj.__dict__)
        else:
            return repr(obj)  # Don't know how to handle, convert to string

    @staticmethod
    def dump(obj):
        return jsonDump.__serialize(obj)


def getclassFromName(cmd, name):
    module = inspect.getmodule(cmd)
    return getattr(module, name)()


def finalizeResultObj(result, responseName, responsecls):
    responsclsLoadable = (responsecls is None
                          and responseName.endswith("response")
                          and responseName != "queryasyncjobresultresponse")
    if responsclsLoadable:
        '''infer the response class from the name'''
        moduleName = responseName.replace("response", "")
        try:
            responsecls = getclassFromName(moduleName, responseName)
        except:
            pass

    responsNameValid = (responseName is not None
                        and responseName == "queryasyncjobresultresponse"
                        and responsecls is not None
                        and result.jobresult is not None)
    if responsNameValid:
        result.jobresult = finalizeResultObj(result.jobresult, None,
                                             responsecls)
        return result
    elif responsecls is not None:
        for k, v in result.__dict__.items():
            if k in responsecls.__dict__:
                return result

        attr = list(result.__dict__.keys())[0]

        value = getattr(result, attr)
        if not isinstance(value, jsonLoader):
            return result

        findObj = False
        for k, v in value.__dict__.items():
            if k in responsecls.__dict__:
                findObj = True
                break
        if findObj:
            return value
        else:
            return result
    else:
        return result


def getResultObj(returnObj, responsecls=None):
    if len(returnObj) == 0:
        return None
    responseName = [a for a in list(returnObj.keys()) if a != 'cloudstack-version'][0]

    response = returnObj[responseName]
    if len(response) == 0:
        return None

    result = jsonLoader(response)
    if result.errorcode is not None:
        errMsg = "errorCode: %s, errorText:%s" % (result.errorcode,
                                                  result.errortext)
        respname = responseName.replace("response", "")
        raise cloudstackException.CloudstackAPIException(respname, errMsg)

    if result.count is not None:
        for key in result.__dict__.keys():
            if key == "count":
                continue
            else:
                return getattr(result, key)
    else:
        return finalizeResultObj(result, responseName, responsecls)

if __name__ == "__main__":

    result = '''{ "listnetworkserviceprovidersresponse" : { "count" : 1,
      "networkserviceprovider" : [ { "destinationphysicalnetworkid" : "0",
            "id" : "d827cae4-4998-4037-95a2-55b92b6318b1",
            "name" : "VirtualRouter",
            "physicalnetworkid" : "ad2948fc-1054-46c7-b1c7-61d990b86710",
            "servicelist" : [ "Vpn",
                "Dhcp",
                "Dns",
                "Gateway",
                "Firewall",
                "Lb",
                "SourceNat",
                "StaticNat",
                "PortForwarding",
                "UserData"
              ],
            "state" : "Disabled"
          } ]
    } }'''
    nsp = getResultObj(result)
    print(nsp[0].id)

    result = '''{ "listzonesresponse" : {
    "count" : 1,
    "zone" : [ { "allocationstate" : "Enabled",
            "dhcpprovider" : "DhcpServer",
            "dns1" : "8.8.8.8",
            "dns2" : "8.8.4.4",
            "id" : 1,
            "internaldns1" : "192.168.110.254",
            "internaldns2" : "192.168.110.253",
            "name" : "test0",
            "networktype" : "Basic",
            "securitygroupsenabled" : true,
            "zonetoken" : "5e818a11-6b00-3429-9a07-e27511d3169a"
        } ]
    }
}'''
    zones = getResultObj(result)
    print(zones[0].id)
    res = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressResponse()
    result = '''{
    "queryasyncjobresultresponse" : {
        "jobid" : 10,
        "jobprocstatus" : 0,
        "jobresult" : {
            "securitygroup" : {
                "account" : "admin",
                "description" : "Default Security Group",
                "domain" : "ROOT",
                "domainid" : 1,
                "id" : 1,
                "ingressrule" : [ { "account" : "a",
                    "endport" : 22,
                    "protocol" : "tcp",
                    "ruleid" : 1,
                    "securitygroupname" : "default",
                    "startport" : 22
                  },
                  { "account" : "b",
                    "endport" : 22,
                    "protocol" : "tcp",
                    "ruleid" : 2,
                    "securitygroupname" : "default",
                    "startport" : 22
                  }
                ],
              "name" : "default"
            }
        },
        "jobresultcode" : 0,
        "jobresulttype" : "object",
        "jobstatus" : 1
    }
}'''
    asynJob = getResultObj(result, res)
    print(asynJob.jobid, repr(asynJob.jobresult))
    print(asynJob.jobresult.ingressrule[0].account)

    result = '''{
    "queryasyncjobresultresponse" : {
        "errorcode" : 431,
        "errortext" :
"Unable to execute API command queryasyncjobresultresponse \
due to missing parameter jobid"
    }
}'''
    try:
        asynJob = getResultObj(result)
    except cloudstackException.CloudstackAPIException as e:
        print(e)

    result = '{ "queryasyncjobresultresponse" : {}  }'
    asynJob = getResultObj(result)
    print(asynJob)

    result = '{}'
    asynJob = getResultObj(result)
    print(asynJob)

    result = '''{
    "createzoneresponse" : {
        "zone" : {
            "id":1,"name":"test0","dns1":"8.8.8.8","dns2":"8.8.4.4",
            "internaldns1":"192.168.110.254","internaldns2":"192.168.110.253",
            "networktype":"Basic","securitygroupsenabled":true,
            "allocationstate":"Enabled",
            "zonetoken":"3442f287-e932-3111-960b-514d1f9c4610",
            "dhcpprovider":"DhcpServer"
        }
    }
}'''
    res = createZone.createZoneResponse()
    zone = getResultObj(result, res)
    print(zone.id)

    result = '{ "attachvolumeresponse" : {"jobid":24} }'
    res = attachVolume.attachVolumeResponse()
    res = getResultObj(result, res)
    print(res)

    result = '{ "listtemplatesresponse" : { } }'
    print(getResultObj(result, listTemplates.listTemplatesResponse()))

    result = '''{
    "queryasyncjobresultresponse" : {
        "jobid":34,"jobstatus":2,"jobprocstatus":0,"jobresultcode":530,
        "jobresulttype":"object","jobresult":{
            "errorcode":431,
            "errortext":
"Please provide either a volume id, or a tuple(device id, instance id)"
        }
    }
}'''
    print(getResultObj(result, listTemplates.listTemplatesResponse()))
    result = '''{
    "queryasyncjobresultresponse" : {
        "jobid":41,"jobstatus":1,"jobprocstatus":0,
        "jobresultcode":0,"jobresulttype":"object","jobresult":{
            "virtualmachine":{
                "id":37,"name":"i-2-37-TEST",
                "displayname":"i-2-37-TEST","account":"admin",
                "domainid":1,"domain":"ROOT",
                "created":"2011-08-25T11:13:42-0700",
                "state":"Running","haenable":false,"zoneid":1,
                "zonename":"test0","hostid":5,
                "hostname":
                    "SimulatedAgent.1e629060-f547-40dd-b792-57cdc4b7d611",
                "templateid":10,
                "templatename":"CentOS 5.3(64-bit) no GUI (Simulator)",
                "templatedisplaytext":
                    "CentOS 5.3(64-bit) no GUI (Simulator)",
                "passwordenabled":false,"serviceofferingid":7,
                "serviceofferingname":"Small Instance","cpunumber":1,
                "cpuspeed":500,"memory":512,"guestosid":11,
                "rootdeviceid":0,"rootdevicetype":"NetworkFilesystem",
                "securitygroup":[{"id":1,"name":"default",
                    "description":"Default Security Group"}],
                "nic":[{"id":43,"networkid":204,
                    "netmask":"255.255.255.0","gateway":"192.168.1.1",
                    "ipaddress":"192.168.1.27",
                    "isolationuri":"ec2://untagged",
                    "broadcasturi":"vlan://untagged",
                    "traffictype":"Guest","type":"Direct",
                    "isdefault":true,"macaddress":"06:56:b8:00:00:53"}],
                "hypervisor":"Simulator"
            }
        }
    }
}'''

    result = '''{
    "queryasyncjobresultresponse" : {
        "accountid":"30910093-22e4-4d3c-a464-8b36b60c8001",
        "userid":"cb0aeca3-42ee-47c4-838a-2cd9053441f2",
        "cmd":"com.cloud.api.commands.DeployVMCmd","jobstatus":1,
        "jobprocstatus":0,"jobresultcode":0,"jobresulttype":"object",
        "jobresult":{
            "virtualmachine":{
                "id":"d2e4d724-e089-4e59-be8e-647674059016",
                "name":"i-2-14-TEST","displayname":"i-2-14-TEST",
                "account":"admin",
                "domainid":"8cfafe79-81eb-445e-8608-c5b7c31fc3a5",
                "domain":"ROOT","created":"2012-01-15T18:30:11+0530",
                "state":"Running","haenable":false,
                "zoneid":"30a397e2-1c85-40c0-8463-70278952b046",
                "zonename":"Sandbox-simulator",
                "hostid":"cc0105aa-a2a9-427a-8ad7-4d835483b8a9",
                "hostname":
                    "SimulatedAgent.9fee20cc-95ca-48b1-8268-5513d6e83a1b",
                "templateid":"d92570fa-bf40-44db-9dff-45cc7042604d",
                "templatename":"CentOS 5.3(64-bit) no GUI (Simulator)",
                "templatedisplaytext":"CentOS 5.3(64-bit) no GUI (Simulator)",
                "passwordenabled":false,
                "serviceofferingid":"3734d632-797b-4f1d-ac62-33f9cf70d005",
                "serviceofferingname":"Sample SO","cpunumber":1,"cpuspeed":100,
                "memory":128,
                "guestosid":"1e36f523-23e5-4e90-869b-a1b5e9ba674d",
                "rootdeviceid":0,"rootdevicetype":"NetworkFilesystem",
                "nic":[{"id":"4d3ab903-f511-4dab-8a6d-c2a3b51de7e0",
                    "networkid":"faeb7f24-a4b9-447d-bec6-c4956c4ab0f6",
                    "netmask":"255.255.240.0","gateway":"10.6.240.1",'
                    "ipaddress":"10.6.253.89","isolationuri":"vlan://211",
                    "broadcasturi":"vlan://211","traffictype":"Guest",
                    "type":"Isolated","isdefault":true,
                    "macaddress":"02:00:04:74:00:09"}],
                "hypervisor":"Simulator"
            }
        },
        "created":"2012-01-15T18:30:11+0530",
        "jobid":"f4a13f28-fcd6-4d7f-b9cd-ba7eb5a5701f"
    }
}'''
    vm = getResultObj(result,
                      deployVirtualMachine.deployVirtualMachineResponse())
    print(vm.jobresult.id)

    cmd = deployVirtualMachine.deployVirtualMachineCmd()
    responsename = cmd.__class__.__name__.replace("Cmd", "Response")
    response = getclassFromName(cmd, responsename)
    print(response.id)
