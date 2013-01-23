import factory
from marvin.integration.lib.newbase import VPCOffering
class VPCOfferingFactory(factory.Factory):

    FACTORY_FOR = VPCOffering

    displaytext = None
    name = None
    supportedservices = None
