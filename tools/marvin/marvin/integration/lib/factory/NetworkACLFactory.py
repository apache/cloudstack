import factory
from marvin.integration.lib.newbase import NetworkACL
class NetworkACLFactory(factory.Factory):

    FACTORY_FOR = NetworkACL

    networkid = None
    protocol = None
