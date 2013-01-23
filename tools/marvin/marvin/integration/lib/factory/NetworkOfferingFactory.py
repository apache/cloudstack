import factory
from marvin.integration.lib.newbase import NetworkOffering
class NetworkOfferingFactory(factory.Factory):

    FACTORY_FOR = NetworkOffering

    displaytext = None
    guestiptype = None
    name = None
    supportedservices = None
    traffictype = None
