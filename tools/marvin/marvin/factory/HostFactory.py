from marvin.base import Host
from marvin.factory import CloudStackBaseFactory

class HostFactory(CloudStackBaseFactory):

    FACTORY_FOR = Host

    hypervisor = None
    password = None
    podid = None
    url = None
    username = None
    zoneid = None


class XenserverHostFactory(HostFactory):

    hypervisor = 'XenServer'
    password = 'password'
    username = 'root'


class KvmHostFactory(HostFactory):

    hypervisor = 'KVM'
    password = 'password'
    username = 'root'
