import factory
from marvin.integration.lib.base import Host
class HostFactory(factory.Factory):

    FACTORY_FOR = Host

    hypervisor = None
    password = None
    podid = None
    url = None
    username = None
    zoneid = None

