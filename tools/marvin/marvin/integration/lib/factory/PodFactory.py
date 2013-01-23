import factory
from marvin.integration.lib.newbase import Pod
class PodFactory(factory.Factory):

    FACTORY_FOR = Pod

    gateway = None
    name = None
    netmask = None
    startip = None
    zoneid = None
