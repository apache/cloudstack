import factory
from marvin.integration.lib.newbase import PhysicalNetwork
class PhysicalNetworkFactory(factory.Factory):

    FACTORY_FOR = PhysicalNetwork

    name = None
    zoneid = None
