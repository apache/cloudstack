import factory
from marvin.integration.lib.newbase import Network
class NetworkFactory(factory.Factory):

    FACTORY_FOR = Network

    displaytext = None
    name = None
    networkofferingid = None
    zoneid = None
