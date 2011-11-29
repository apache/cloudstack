import cloudstackException
import json
import inspect
from cloudstackAPI import * 
import pdb

class jsonLoader:
    '''The recursive class for building and representing objects with.'''
    def __init__(self, obj):
        for k in obj:
            v = obj[k]
            if isinstance(v, dict):
                setattr(self, k, jsonLoader(v))
            elif isinstance(v, (list, tuple)):
                setattr(self, k, [jsonLoader(elem) for elem in v])
            else:
                setattr(self,k,v)
    def __getattr__(self, val):
        if val in self.__dict__:
            return self.__dict__[val]
        else:
            return None
    def __repr__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v) in self.__dict__.iteritems()))
    def __str__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v) in self.__dict__.iteritems()))
    
    
class jsonDump:
    @staticmethod
    def __serialize(obj):
        """Recursively walk object's hierarchy."""
        if isinstance(obj, (bool, int, long, float, basestring)):
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
            return repr(obj) # Don't know how to handle, convert to string
        
    @staticmethod
    def dump(obj):
        return jsonDump.__serialize(obj)

def getclassFromName(cmd, name):
    module = inspect.getmodule(cmd)
    return getattr(module, name)()

def finalizeResultObj(result, responseName, responsecls):
    if responsecls is None and responseName.endswith("response") and responseName != "queryasyncjobresultresponse":
        '''infer the response class from the name'''
        moduleName = responseName.replace("response", "")
        try:
            responsecls = getclassFromName(moduleName, responseName)
        except:
            pass
    
    if responseName is not None and responseName == "queryasyncjobresultresponse" and responsecls is not None and result.jobresult is not None:
        result.jobresult = finalizeResultObj(result.jobresult, None, responsecls)
        return result
    elif responsecls is not None:
        for k,v in result.__dict__.iteritems():
            if k in responsecls.__dict__:
                return result
        
        attr = result.__dict__.keys()[0]
       
        value = getattr(result, attr)
        if not isinstance(value, jsonLoader):
            return result
        
        findObj = False
        for k,v in value.__dict__.iteritems():
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
    returnObj = json.loads(returnObj)
    
    if len(returnObj) == 0:
        return None
    responseName = returnObj.keys()[0]
    
    response = returnObj[responseName]
    if len(response) == 0:
        return None
    
    result = jsonLoader(response)
    if result.errorcode is not None:
        errMsg = "errorCode: %s, errorText:%s"%(result.errorcode, result.errortext)
        raise cloudstackException.cloudstackAPIException(responseName.replace("response", ""), errMsg)
    
    if result.count is not None:
        for key in result.__dict__.iterkeys():
            if key == "count":
                continue
            else:
                return getattr(result, key)
    else:
        return finalizeResultObj(result, responseName, responsecls)
    
if __name__ == "__main__":

    result = '{ "listnetworkserviceprovidersresponse" : { "count":1 ,"networkserviceprovider" : [ {"name":"VirtualRouter","physicalnetworkid":"ad2948fc-1054-46c7-b1c7-61d990b86710","destinationphysicalnetworkid":"0","state":"Disabled","id":"d827cae4-4998-4037-95a2-55b92b6318b1","servicelist":["Vpn","Dhcp","Dns","Gateway","Firewall","Lb","SourceNat","StaticNat","PortForwarding","UserData"]} ] } }'
    nsp = getResultObj(result)
    
    result = '{ "listzonesresponse" : { "count":1 ,"zone" : [  {"id":1,"name":"test0","dns1":"8.8.8.8","dns2":"4.4.4.4","internaldns1":"192.168.110.254","internaldns2":"192.168.110.253","networktype":"Basic","securitygroupsenabled":true,"allocationstate":"Enabled","zonetoken":"5e818a11-6b00-3429-9a07-e27511d3169a","dhcpprovider":"DhcpServer"} ] } }'
    zones = getResultObj(result)
    print zones[0].id
    res = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressResponse()
    result = '{ "queryasyncjobresultresponse" : {"jobid":10,"jobstatus":1,"jobprocstatus":0,"jobresultcode":0,"jobresulttype":"object","jobresult":{"securitygroup":{"id":1,"name":"default","description":"Default Security Group","account":"admin","domainid":1,"domain":"ROOT","ingressrule":[{"ruleid":1,"protocol":"tcp","startport":22,"endport":22,"securitygroupname":"default","account":"a"},{"ruleid":2,"protocol":"tcp","startport":22,"endport":22,"securitygroupname":"default","account":"b"}]}}} }'
    asynJob = getResultObj(result, res)
    print asynJob.jobid, repr(asynJob.jobresult)
    print asynJob.jobresult.ingressrule[0].account
    
    result = '{ "queryasyncjobresultresponse" : {"errorcode" : 431, "errortext" : "Unable to execute API command queryasyncjobresultresponse due to missing parameter jobid"}  }'
    try:
        asynJob = getResultObj(result)
    except cloudstackException.cloudstackAPIException, e:
        print e
    
    result = '{ "queryasyncjobresultresponse" : {}  }'
    asynJob = getResultObj(result)
    print asynJob
    
    result = '{}'
    asynJob = getResultObj(result)
    print asynJob
    
    result = '{ "createzoneresponse" :  { "zone" : {"id":1,"name":"test0","dns1":"8.8.8.8","dns2":"4.4.4.4","internaldns1":"192.168.110.254","internaldns2":"192.168.110.253","networktype":"Basic","securitygroupsenabled":true,"allocationstate":"Enabled","zonetoken":"3442f287-e932-3111-960b-514d1f9c4610","dhcpprovider":"DhcpServer"} }  }'
    res = createZone.createZoneResponse()
    zone = getResultObj(result, res)
    print zone.id
    
    result = '{ "attachvolumeresponse" : {"jobid":24} }'
    res = attachVolume.attachVolumeResponse()
    res = getResultObj(result, res)
    print res
    
    result = '{ "listtemplatesresponse" : { } }'
    print getResultObj(result, listTemplates.listTemplatesResponse())
    
    result = '{ "queryasyncjobresultresponse" : {"jobid":34,"jobstatus":2,"jobprocstatus":0,"jobresultcode":530,"jobresulttype":"object","jobresult":{"errorcode":431,"errortext":"Please provide either a volume id, or a tuple(device id, instance id)"}} }'
    print getResultObj(result, listTemplates.listTemplatesResponse())
    result = '{ "queryasyncjobresultresponse" : {"jobid":41,"jobstatus":1,"jobprocstatus":0,"jobresultcode":0,"jobresulttype":"object","jobresult":{"virtualmachine":{"id":37,"name":"i-2-37-TEST","displayname":"i-2-37-TEST","account":"admin","domainid":1,"domain":"ROOT","created":"2011-08-25T11:13:42-0700","state":"Running","haenable":false,"zoneid":1,"zonename":"test0","hostid":5,"hostname":"SimulatedAgent.1e629060-f547-40dd-b792-57cdc4b7d611","templateid":10,"templatename":"CentOS 5.3(64-bit) no GUI (Simulator)","templatedisplaytext":"CentOS 5.3(64-bit) no GUI (Simulator)","passwordenabled":false,"serviceofferingid":7,"serviceofferingname":"Small Instance","cpunumber":1,"cpuspeed":500,"memory":512,"guestosid":11,"rootdeviceid":0,"rootdevicetype":"NetworkFilesystem","securitygroup":[{"id":1,"name":"default","description":"Default Security Group"}],"nic":[{"id":43,"networkid":204,"netmask":"255.255.255.0","gateway":"192.168.1.1","ipaddress":"192.168.1.27","isolationuri":"ec2://untagged","broadcasturi":"vlan://untagged","traffictype":"Guest","type":"Direct","isdefault":true,"macaddress":"06:56:b8:00:00:53"}],"hypervisor":"Simulator"}}} }'
    vm = getResultObj(result, deployVirtualMachine.deployVirtualMachineResponse())
    print vm.jobresult.id
    
    cmd = deployVirtualMachine.deployVirtualMachineCmd()
    responsename = cmd.__class__.__name__.replace("Cmd", "Response")
    response = getclassFromName(cmd, responsename)
    print response.id
