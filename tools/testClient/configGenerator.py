import json
import os
class Struct:
    '''The recursive class for building and representing objects with.'''
    def __init__(self, obj):
        for k in obj:
            v = obj[k]
            if isinstance(v, dict):
                setattr(self, k, Struct(v))
            elif isinstance(v, (list, tuple)):
                setattr(self, k, [Struct(elem) for elem in v])
            else:
                setattr(self,k,v)
    def __getattr__(self, val):
        if val in self.__dict__:
            return self.__dict__[val]
        else:
            return None
    def __repr__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v) in self.__dict__.iteritems()))
    
    
def json_repr(obj):
    """Represent instance of a class as JSON.
    Arguments:
    obj -- any object
    Return:
    String that reprent JSON-encoded object.
    """
    def serialize(obj):
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
                    newobj[key] = serialize(obj[key])
                
            return newobj
        elif isinstance(obj, list):
            return [serialize(item) for item in obj]
        elif isinstance(obj, tuple):
            return tuple(serialize([item for item in obj]))
        elif hasattr(obj, '__dict__'):
            return serialize(obj.__dict__)
        else:
            return repr(obj) # Don't know how to handle, convert to string
    return serialize(obj)

class managementServer():
    def __init__(self):
        self.mgtSvrIp = None
        self.port = 8096
        self.apiKey = None
        self.securityKey = None
        
class dbServer():
    def __init__(self):
        self.dbSvr = None
        self.port = 3306
        self.user = "cloud"
        self.passwd = ""
        self.db = "cloud"

class configuration():
    def __init__(self):
        self.name = None
        self.value = None
class logger():
    def __init__(self):
        '''TestCase/TestClient'''
        self.name = None
        self.file = None

class cloudstackConfiguration():
    def __init__(self):
        self.zones = []
        self.mgtSvr = []
        self.dbSvr = None
        self.globalConfig = []
        self.logger = []

class zone():
    def __init__(self):
        self.dns1 = None
        self.internaldns1 = None
        self.name = None
        '''Basic or Advanced'''
        self.networktype = None
        self.dns2 = None
        self.guestcidraddress = None
        self.internaldns2 = None
        self.securitygroupenabled = None
        '''guest vlan: e.g. 100-200, used in advanced mode'''
        self.vlan = None
        '''default public network, in advanced mode'''
        self.ipranges = []
        '''tagged network, in advanced mode'''
        self.networks = []
        self.pods = []
        self.secondaryStorages = []
        
class pod():
    def __init__(self):
        self.gateway = None
        self.name = None
        self.netmask = None
        self.startip = None
        self.endip = None
        self.zoneid = None
        self.clusters = []
        '''Used in basic network mode'''
        self.guestIpRanges = []
        self.primaryStorages = []

class cluster():
    def __init__(self):
        self.clustername = None
        self.clustertype = None
        self.hypervisor = None
        self.zoneid = None
        self.podid = None
        self.password = None
        self.url = None
        self.username = None
        self.hosts = []
  
class host():
    def __init__(self):
        self.hypervisor = None
        self.password = None
        self.url = None
        self.username = None
        self.zoneid = None
        self.podid = None
        self.clusterid = None
        self.clustername = None
        self.cpunumber = None
        self.cpuspeed = None
        self.hostmac = None
        self.hosttags = None
        self.memory = None
    
class network():
    def __init__(self):
        self.displaytext = None
        self.name = None
        self.zoneid = None
        self.account = None
        self.domainid = None
        self.isdefault = None
        self.isshared = None
        self.networkdomain = None
        self.networkofferingid = None
        self.ipranges = []
      
class iprange():
    def __init__(self):
        '''tagged/untagged'''
        self.vlan = None
        self.gateway = None
        self.netmask = None
        self.startip = None
        self.endip = None
        '''for account specific '''
        self.account = None
        self.domain = None

class primaryStorage():
    def __init__(self):
        self.name = None
        self.url = None

class secondaryStorage():
    def __init__(self):
        self.url = None
  
'''sample code to generate setup configuration file'''
def describe_setup_in_basic_mode():
    zs = cloudstackConfiguration()
    
    z = zone()
    z.dns1 = "8.8.8.8"
    z.dns2 = "4.4.4.4"
    z.internaldns1 = "192.168.110.254"
    z.internaldns2 = "192.168.110.253"
    z.guestcidraddress = "10.1.1.0/24"
    z.name = "test"
    z.networktype = 'Basic'
    
    '''create 10 pods'''
    for i in range(1):
        p = pod()
        p.name = "test" +str(i)
        p.gateway = "192.168.%d.1"%i
        p.netmask = "255.255.255.0"
        p.startip = "192.168.%d.200"%i
        p.endip = "192.168.%d.220"%i
        
        '''add two pod guest ip ranges'''
        for j in range(1):
            ip = iprange()
            ip.gateway = p.gateway
            ip.netmask = p.netmask
            ip.startip = "192.168.%d.2"%i
            ip.endip = "192.168.%d.100"%i
            
            p.guestIpRanges.append(ip)
        
        '''add 10 clusters'''
        for j in range(1):
            c = cluster()
            c.clustername = "test"+str(i) + str(j)
            c.clustertype = "CloudManaged"
            c.hypervisor = "Simulator"
            
            '''add 10 hosts'''
            for k in range(1):
                h = host()
                h.clustername = "host"+str(i) + str(j) + str(k)
                h.username = "root"
                h.password = "password"
                h.url = "http://Sim"
                c.hosts.append(h)
                
            p.clusters.append(c)
            
        '''add 2 primary storages'''
        for j in range(2):
            primary = primaryStorage()
            primary.name = "primary"+str(j)
            primary.url = "nfs://localhost/path"+str(i) + str(j)
            p.primaryStorages.append(primary)
            
        z.pods.append(p)
            
    '''add two secondary'''
    for i in range(2):
        secondary = secondaryStorage()
        secondary.url = "nfs://localhost/path"+str(i)
        z.secondaryStorages.append(secondary)
        
    zs.zones.append(z)
    
    '''Add one mgt server'''
    mgt = managementServer()
    mgt.mgtSvrIp = "localhost"
    zs.mgtSvr.append(mgt)
    
    '''Add a database'''
    db = dbServer()
    db.dbSvr = "localhost"
    
    zs.dbSvr = db
    
    ''''add loggers'''
    testClientLogger = logger()
    testClientLogger.name = "TestClient"
    testClientLogger.file = "/var/log/cloud/testclient.log"
    
    testCaseLogger = logger()
    testCaseLogger.name = "TestCase"
    testCaseLogger.file = "/var/log/cloud/testcase.log"
    
    zs.logger.append(testClientLogger)
    zs.logger.append(testCaseLogger)
    
    return zs

def generate_setup_config(func, file=None):
    describe = func()
    if file is None:
        return json.dumps(json_repr(describe))
    else:
        fp = open(file, 'w')
        json.dump(json_repr(describe), fp, indent=4)
        fp.close()
        
    
def get_setup_config(file):
    if not os.path.exists(file):
        return None
    config = cloudstackConfiguration()
    fp = open(file, 'r')
    config = json.load(fp)
    return config

if __name__ == "__main__":
    generate_setup_config(describe_setup_in_basic_mode, "/tmp/x.json")
